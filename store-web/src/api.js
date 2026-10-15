export class ApiError extends Error {
  constructor(status, body) {
    super((body && body.message) || 'Something went wrong. Please try again.');
    this.status = status;
    this.code = body && body.error;
    this.details = body && body.details;
  }
}

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

// BASE_URL is "/" locally and the app's folder (for example "/grocery/") when hosted under one.
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
      throw new ApiError(0, { message: 'Could not reach the store. Check your connection and try again.' });
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

/** Sends a form that may carry a file. The browser sets the multipart content type itself. */
function upload(path, fields) {
  const form = new FormData();
  Object.entries(fields).forEach(([name, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      form.append(name, value);
    }
  });
  return request(path, { method: 'POST', body: form });
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

export const api = {
  hubs: () => request('/hubs'),
  storeInfo: () => request('/store'),
  catalog: (hubId) => request(`/hubs/${hubId}/catalog`),
  placeOrder: (order) => post('/orders', order),
  order: (orderNumber) => request(`/orders/${encodeURIComponent(orderNumber)}`),
  advanceOrder: (orderNumber) => post(`/orders/${encodeURIComponent(orderNumber)}/advance`),
  hubOrders: (hubId) => request(`/hubs/${hubId}/orders`),
  stock: (hubId) => request(`/hubs/${hubId}/stock`),
  receiveStock: (hubId, receipt) => post(`/hubs/${hubId}/stock/receipts`, receipt),
  product: (hubId, productId) => request(`/hubs/${hubId}/products/${productId}`),
  setGrowing: (productId, fields) => upload(`/products/${productId}/growing`, fields),
  recordBatchTest: (hubId, batchId, fields) => upload(`/hubs/${hubId}/stock/batches/${batchId}/test`, fields),
  candidates: () => request('/catalog/candidates'),
  approveCandidate: (hubId, candidateId, details) => post(`/hubs/${hubId}/catalog/candidates/${candidateId}/approve`, details),
  rejectCandidate: (candidateId) => post(`/catalog/candidates/${candidateId}/reject`),
  signUp: (details) => post('/auth/signup', details),
  confirmSignUp: (signupId, code) => post('/auth/signup/confirm', { signupId, code }),
  signIn: (details) => post('/auth/signin', details),
  signOut: () => post('/auth/signout'),
  me: () => request('/me'),
  signInOptions: () => request('/auth/options'),
  googleSignIn: (credential) => post('/auth/google', { credential }),
  appleSignIn: (idToken, name) => post('/auth/apple', { idToken, name }),
  sendEmailCode: (email) => post('/auth/email/code', { email }),
  verifyEmailCode: (codeId, code) => post('/auth/email/verify', { codeId, code }),
  finishEmailSignUp: (details) => post('/auth/email/finish', details),
  passkeySignInOptions: () => post('/auth/passkey/options'),
  passkeySignIn: (ceremonyId, credentialJson) => post('/auth/passkey', { ceremonyId, credentialJson }),
  passkeys: () => request('/me/passkeys'),
  passkeyOptions: () => post('/me/passkeys/options'),
  addPasskey: (ceremonyId, credentialJson, label) => post('/me/passkeys', { ceremonyId, credentialJson, label }),
  removePasskey: (passkeyId) => request(`/me/passkeys/${passkeyId}`, { method: 'DELETE' }),
  assistantMemory: () => request('/me/assistant-memory'),
  forgetMemory: (factId) => request(`/me/assistant-memory/${factId}`, { method: 'DELETE' }),
  forgetAllMemory: () => request('/me/assistant-memory', { method: 'DELETE' }),
  savedCart: () => request('/me/cart'),
  saveCart: (lines) => put('/me/cart', { lines }),
  askAssistant: (hubId, message, history) => post(`/hubs/${hubId}/assistant`, { message, history })
};
