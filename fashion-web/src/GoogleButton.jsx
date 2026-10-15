import React, { useEffect, useRef, useState } from 'react';
import { api } from './api';

// Google's own button script. Loaded only when Google sign-in is switched on.
let scriptLoading = null;
function loadGoogle() {
  if (!scriptLoading) {
    scriptLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.onload = resolve;
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }
  return scriptLoading;
}

/**
 * "Continue with Google". Google gives the browser a signed token, which the API checks before signing in.
 * Renders nothing while Google sign-in is off (no client id set on the API) or if Google cannot be reached.
 */
export default function GoogleButton({ onCredential }) {
  const holder = useRef(null);
  const [clientId, setClientId] = useState(null);
  const latest = useRef(onCredential);
  latest.current = onCredential;

  useEffect(() => {
    api.signInOptions().then((options) => setClientId(options.googleClientId || null)).catch(() => {});
  }, []);

  useEffect(() => {
    if (!clientId) {
      return;
    }
    let cancelled = false;
    loadGoogle().then(() => {
      if (cancelled || !holder.current) {
        return;
      }
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => latest.current(response.credential),
        ux_mode: 'popup',
        // Google's own prompt about third-party sign-in, shown where browsers need it.
        use_fedcm_for_prompt: true
      });
      window.google.accounts.id.renderButton(holder.current, {
        theme: 'outline', size: 'large', text: 'continue_with', shape: 'rectangular',
        width: Math.min(holder.current.offsetWidth || 400, 400)
      });
    }).catch(() => {});
    return () => { cancelled = true; };
  }, [clientId]);

  if (!clientId) {
    return null;
  }
  return (
    <>
      <div ref={holder} className="google-button" />
      <p className="or"><span>or</span></p>
    </>
  );
}
