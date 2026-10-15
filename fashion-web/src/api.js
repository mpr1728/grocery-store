export class ApiError extends Error {
  constructor(status, body) {
    super((body && body.message) || 'Something went wrong. Please try again.');
    this.status = status;
    this.code = body && body.error;
    this.details = body && body.details;
  }
}

// The same key the grocery front end uses, so a sign-in there also counts here while both share one site.
const TOKEN_KEY = 'session.v1';
let sessionToken = null;
try {
  sessionToken = localStorage.getItem(TOKEN_KEY);
} catch (e) {
  // Without storage the session lasts until the page is closed.
}
let onSessionEnded = () => {};

export function setSessionToken(token) {
  sessionToken = token;
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  } catch (e) {
    // Signing in still works for this visit.
  }
}

export function hasSession() {
  return sessionToken !== null;
}

/** Called when the server says the saved session is no longer valid. */
export function whenSessionEnds(handler) {
  onSessionEnded = handler;
}

// A 429 means the server was too busy to start on the request, so it is safe to send again.
const BUSY_RETRY_MS = 500;
const BUSY_GIVE_UP_MS = 25000;
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// BASE_URL is "/" locally and the app's folder (for example "/fashion/") when hosted under one.
const API_ROOT = import.meta.env.BASE_URL + 'api';

async function request(path, options) {
  const headers = { ...(options && options.headers) };
  if (sessionToken) {
    headers.Authorization = 'Bearer ' + sessionToken;
  }
  const started = Date.now();
  let response;
  while (true) {
    try {
      response = await fetch(API_ROOT + path, { ...options, headers });
    } catch (e) {
      throw new ApiError(0, { message: 'Could not reach Panyaloka. Check your connection and try again.' });
    }
    if (response.status !== 429 || Date.now() - started > BUSY_GIVE_UP_MS) {
      break;
    }
    await wait(BUSY_RETRY_MS);
  }
  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch (e) {
      body = null;
    }
  }
  if (response.status === 401 && sessionToken) {
    setSessionToken(null);
    onSessionEnded();
  }
  if (!response.ok) {
    throw new ApiError(response.status, body);
  }
  return body;
}

function put(path, payload) {
  return request(path, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
}

function post(path, payload) {
  return request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: payload === undefined ? undefined : JSON.stringify(payload)
  });
}

// Accounts are shared with Panyaloka Fresh: same endpoints, same saved session.
export const api = {
  signUp: (details) => post('/auth/signup', details),
  confirmSignUp: (signupId, code) => post('/auth/signup/confirm', { signupId, code }),
  signIn: (details) => post('/auth/signin', details),
  signOut: () => post('/auth/signout'),
  me: () => request('/me'),
  sendEmailCode: (email) => post('/auth/email/code', { email }),
  verifyEmailCode: (codeId, code) => post('/auth/email/verify', { codeId, code }),
  finishEmailSignUp: (details) => post('/auth/email/finish', details),
  passkeySignInOptions: () => post('/auth/passkey/options'),
  passkeySignIn: (ceremonyId, credentialJson) => post('/auth/passkey', { ceremonyId, credentialJson }),
  passkeys: () => request('/me/passkeys'),
  passkeyOptions: () => post('/me/passkeys/options'),
  addPasskey: (ceremonyId, credentialJson, label) => post('/me/passkeys', { ceremonyId, credentialJson, label }),
  removePasskey: (passkeyId) => request(`/me/passkeys/${passkeyId}`, { method: 'DELETE' }),
  catalog: () => request('/fashion/catalog'),
  quote: (design) => post('/fashion/quote', design),
  fit: (measurements) => post('/fashion/fit', measurements),
  savedFit: () => request('/fashion/me/measurements'),
  saveFit: (measurements) => put('/fashion/me/measurements', measurements),
  placeOrder: (order) => post('/fashion/orders', order),
  myOrders: () => request('/fashion/me/orders'),
  order: (orderNumber) => request(`/fashion/orders/${encodeURIComponent(orderNumber)}`),
  cancelOrder: (orderNumber) => post(`/fashion/orders/${encodeURIComponent(orderNumber)}/cancel`),
  studioOrders: () => request('/fashion/studio/orders'),
  advanceOrder: (orderNumber) => post(`/fashion/studio/orders/${encodeURIComponent(orderNumber)}/advance`)
};
