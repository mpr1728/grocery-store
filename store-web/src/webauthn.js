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

import { useEffect, useState } from 'react';

/**
 * What this device unlocks with, in the words its owner knows. Browsers do not say which sensor a device has, so
 * this goes by the kind of device; an iPhone may have Face ID or Touch ID, so both are named.
 */
function unlockFor(ua, touchScreen) {
  if (/iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && touchScreen)) {
    // iPads report themselves as Macs but have a touch screen.
    return { name: 'Face ID or Touch ID', button: 'Face ID or Touch ID', phone: true };
  }
  if (/Macintosh/.test(ua)) {
    return { name: 'Touch ID', button: 'Touch ID' };
  }
  if (/Android/.test(ua)) {
    return { name: 'your fingerprint or face unlock', button: 'fingerprint or face', phone: true };
  }
  if (/Windows/.test(ua)) {
    return { name: 'Windows Hello (face, fingerprint, or PIN)', button: 'Windows Hello' };
  }
  return { name: 'your screen lock', button: 'a passkey' };
}

/** When the device has no unlock of its own, a passkey still works from a phone (QR code) or a security key. */
const ELSEWHERE = { name: 'your phone (scan a QR code) or a security key', button: 'a passkey', elsewhere: true };

/**
 * How this device signs in with a passkey: { name, button, elsewhere }. Starts with a guess from the device type and
 * corrects it once the browser says whether a built-in unlock is set up.
 */
export function useUnlockMethod() {
  const guess = typeof navigator === 'undefined' ? ELSEWHERE : unlockFor(navigator.userAgent, navigator.maxTouchPoints > 1);
  const [method, setMethod] = useState(guess);
  useEffect(() => {
    if (!passkeysSupported() || !PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable) {
      return;
    }
    PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
      .then((builtIn) => setMethod(builtIn ? unlockFor(navigator.userAgent, navigator.maxTouchPoints > 1) : ELSEWHERE))
      .catch(() => {});
  }, []);
  return method;
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
