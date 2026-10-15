// Browser side of passkeys. The API sends WebAuthn options as JSON with binary fields in base64url; the browser
// needs those as bytes, and its answer goes back the other way.

function fromBase64Url(text) {
  const base64 = text.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - (text.length % 4)) % 4);
  return Uint8Array.from(atob(base64), (c) => c.charCodeAt(0)).buffer;
}

function toBase64Url(buffer) {
  let binary = '';
  new Uint8Array(buffer).forEach((b) => { binary += String.fromCharCode(b); });
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function passkeysSupported() {
  return typeof window !== 'undefined' && !!window.PublicKeyCredential && !!navigator.credentials;
}

/** A short name for this device, so the customer can tell their passkeys apart. */
export function deviceLabel() {
  const ua = navigator.userAgent;
  const device = /iPhone/.test(ua) ? 'iPhone' : /iPad/.test(ua) ? 'iPad' : /Android/.test(ua) ? 'Android'
    : /Macintosh/.test(ua) ? 'Mac' : /Windows/.test(ua) ? 'Windows' : /Linux/.test(ua) ? 'Linux' : 'This device';
  const browser = /Edg\//.test(ua) ? 'Edge' : /Chrome\//.test(ua) ? 'Chrome' : /Firefox\//.test(ua) ? 'Firefox'
    : /Safari\//.test(ua) ? 'Safari' : '';
  return browser ? `${device}, ${browser}` : device;
}

/**
 * Asks for a new passkey. The browser lets the customer keep it on this device, in their password manager, on a
 * phone (by scanning a QR code), or on a security key. Returns the answer as JSON for the API.
 */
export async function createPasskey(optionsJson) {
  const { publicKey } = JSON.parse(optionsJson);
  publicKey.challenge = fromBase64Url(publicKey.challenge);
  publicKey.user.id = fromBase64Url(publicKey.user.id);
  (publicKey.excludeCredentials || []).forEach((c) => { c.id = fromBase64Url(c.id); });
  const credential = await navigator.credentials.create({ publicKey });
  return JSON.stringify({
    id: credential.id,
    rawId: toBase64Url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: toBase64Url(credential.response.clientDataJSON),
      attestationObject: toBase64Url(credential.response.attestationObject),
      transports: credential.response.getTransports ? credential.response.getTransports() : []
    },
    // "platform" when kept on this device, "cross-platform" for a phone or security key.
    authenticatorAttachment: credential.authenticatorAttachment || undefined,
    clientExtensionResults: credential.getClientExtensionResults()
  });
}

/** A name for a new passkey: this device, or a phone or security key used through the browser's prompt. */
export function passkeyLabel(answerJson) {
  return JSON.parse(answerJson).authenticatorAttachment === 'cross-platform' ? 'Phone or security key' : deviceLabel();
}

/** Asks the device to sign in with any passkey it holds for this site. Returns the answer as JSON for the API. */
export async function usePasskey(optionsJson) {
  const { publicKey } = JSON.parse(optionsJson);
  publicKey.challenge = fromBase64Url(publicKey.challenge);
  (publicKey.allowCredentials || []).forEach((c) => { c.id = fromBase64Url(c.id); });
  const credential = await navigator.credentials.get({ publicKey });
  const r = credential.response;
  return JSON.stringify({
    id: credential.id,
    rawId: toBase64Url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: toBase64Url(r.clientDataJSON),
      authenticatorData: toBase64Url(r.authenticatorData),
      signature: toBase64Url(r.signature),
      userHandle: r.userHandle ? toBase64Url(r.userHandle) : null
    },
    clientExtensionResults: credential.getClientExtensionResults()
  });
}

/** True when the customer closed the device prompt, which is not an error worth showing. */
export function wasCancelled(error) {
  return error && (error.name === 'NotAllowedError' || error.name === 'AbortError');
}
