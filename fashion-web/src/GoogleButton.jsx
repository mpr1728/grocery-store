import React, { useEffect, useRef } from 'react';

// Google's own button script, loaded only when Google sign-in is switched on.
let scriptLoading = null;
function loadGoogle() {
  if (!scriptLoading) {
    scriptLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.onload = resolve;
      script.onerror = () => { scriptLoading = null; reject(); };
      document.head.appendChild(script);
    });
  }
  return scriptLoading;
}

/** "Continue with Google". Google gives the browser a signed token, which the API checks before signing in. */
export default function GoogleButton({ clientId, onCredential }) {
  const holder = useRef(null);
  const latest = useRef(onCredential);
  latest.current = onCredential;

  useEffect(() => {
    let cancelled = false;
    loadGoogle().then(() => {
      if (cancelled || !holder.current) {
        return;
      }
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => latest.current(response.credential),
        ux_mode: 'popup',
        use_fedcm_for_prompt: true
      });
      window.google.accounts.id.renderButton(holder.current, {
        theme: 'outline', size: 'large', text: 'continue_with', shape: 'rectangular',
        width: Math.min(holder.current.offsetWidth || 400, 400)
      });
    }).catch(() => {});
    return () => { cancelled = true; };
  }, [clientId]);

  return <div ref={holder} className="google-button" />;
}
