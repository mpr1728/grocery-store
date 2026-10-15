import React, { useEffect, useRef, useState } from 'react';

const SCRIPT = 'https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js';
// The Apple logo, as Apple's design rules ask for on this button.
const LOGO = 'M12.152 6.896c-.948 0-2.415-1.078-3.96-1.04-2.04.027-3.91 1.183-4.961 3.014-2.117 3.675-.546 9.103 1.519 12.09 1.013 1.454 2.208 3.09 3.792 3.039 1.52-.065 2.09-.987 3.935-.987 1.831 0 2.35.987 3.96.948 1.637-.026 2.676-1.48 3.676-2.948 1.156-1.688 1.636-3.325 1.662-3.415-.039-.013-3.182-1.221-3.22-4.857-.026-3.04 2.48-4.494 2.597-4.559-1.429-2.09-3.623-2.324-4.39-2.376-2-.156-3.675 1.09-4.61 1.09zM15.53 3.83c.843-1.012 1.4-2.427 1.245-3.83-1.207.052-2.662.805-3.532 1.818-.78.896-1.454 2.338-1.273 3.714 1.338.104 2.715-.688 3.559-1.701';

let scriptLoading = null;
function loadApple() {
  if (!scriptLoading) {
    scriptLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = SCRIPT;
      script.async = true;
      script.onload = resolve;
      script.onerror = () => { scriptLoading = null; reject(); };
      document.head.appendChild(script);
    });
  }
  return scriptLoading;
}

/**
 * "Continue with Apple". Apple's popup returns a signed token, and on the very first sign-in only, the person's
 * name, which is passed on so the new account can use it. People may hide their email; Apple then gives a relay
 * address that forwards to them.
 */
export default function AppleButton({ clientId, onToken, onFailure }) {
  const [ready, setReady] = useState(false);
  const latest = useRef({ onToken, onFailure });
  latest.current = { onToken, onFailure };

  useEffect(() => {
    let cancelled = false;
    loadApple().then(() => {
      if (cancelled) {
        return;
      }
      window.AppleID.auth.init({
        clientId,
        scope: 'name email',
        // Must be listed as a return URL on the Services ID, even though the popup is used.
        redirectURI: window.location.origin + import.meta.env.BASE_URL + 'account',
        usePopup: true
      });
      setReady(true);
    }).catch(() => {});
    return () => { cancelled = true; };
  }, [clientId]);

  async function signIn() {
    try {
      const result = await window.AppleID.auth.signIn();
      const name = result.user && result.user.name
        ? [result.user.name.firstName, result.user.name.lastName].filter(Boolean).join(' ')
        : null;
      latest.current.onToken(result.authorization.id_token, name);
    } catch (e) {
      if (!e || e.error !== 'popup_closed_by_user') {
        latest.current.onFailure('Apple sign-in did not finish. Please try again, or use an email code.');
      }
    }
  }

  return (
    <button type="button" className="apple-button" onClick={signIn} disabled={!ready}>
      <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d={LOGO} fill="currentColor" /></svg>
      Continue with Apple
    </button>
  );
}
