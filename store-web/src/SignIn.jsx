import React, { useState } from 'react';
import { api } from './api';
import PhoneField, { phoneIsEmpty, phoneIsValid } from './PhoneField';
import { passkeysSupported, usePasskey, useUnlockMethod, wasCancelled } from './webauthn';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function explain(e) {
  return e.code === 'VALIDATION_FAILED' ? 'Please check your details: ' + (e.details || []).join('; ') : e.message;
}

/**
 * One way in for new and returning customers: a code sent to any email address (Gmail, iCloud, Yahoo, or any
 * other), or a passkey. No password to remember or reset. Accounts made with a password can still use it.
 */
export default function SignIn({ onSignedIn, brand }) {
  // email: ask for the address · code: enter the emailed code · profile: new customer gives a name · password
  const [step, setStep] = useState('email');
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [working, setWorking] = useState(false);
  const [problem, setProblem] = useState(null);
  const [note, setNote] = useState(null);
  const unlock = useUnlockMethod();

  async function attempt(action) {
    setWorking(true);
    setProblem(null);
    setNote(null);
    try {
      await action();
    } catch (e) {
      if (!wasCancelled(e)) {
        // Errors from the API carry a status. Anything else came from the device's passkey prompt.
        setProblem(e.status !== undefined ? explain(e) : 'Your device could not use a passkey here. Use an email code instead.');
      }
    } finally {
      setWorking(false);
    }
  }

  const sendCode = (event) => {
    if (event) {
      event.preventDefault();
    }
    return attempt(async () => {
      setSent(await api.sendEmailCode(email.trim()));
      setCode('');
      setStep('code');
    });
  };

  const verify = (event) => {
    event.preventDefault();
    return attempt(async () => {
      const result = await api.verifyEmailCode(sent.codeId, code);
      if (result.needsProfile) {
        setStep('profile');
      } else {
        onSignedIn(result);
      }
    });
  };

  const createAccount = (event) => {
    event.preventDefault();
    return attempt(async () => {
      onSignedIn(await api.finishEmailSignUp({ codeId: sent.codeId, name: name.trim(), phone: phoneIsEmpty(phone) ? null : phone }));
    });
  };

  const withPasskey = () => attempt(async () => {
    const options = await api.passkeySignInOptions();
    const answer = await usePasskey(options.optionsJson);
    onSignedIn(await api.passkeySignIn(options.ceremonyId, answer));
  });

  const withPassword = (event) => {
    event.preventDefault();
    return attempt(async () => onSignedIn(await api.signIn({ login: login.trim(), password })));
  };

  const back = () => { setStep('email'); setProblem(null); setNote(null); };
  const error = problem && <p className="notice error" role="alert">{problem}</p>;

  if (step === 'code') {
    return (
      <div className="narrow">
        <h2>Check your email</h2>
        <p className="hint">We sent a 6 digit code to <b>{sent.sentTo}</b>. It works for 10 minutes. Look in spam or promotions if you do not see it.</p>
        <form onSubmit={verify} className="checkout">
          <label>Code
            <input className="code" value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              inputMode="numeric" autoComplete="one-time-code" placeholder="6 digits" autoFocus required />
          </label>
          {note && <p className="hint">{note}</p>}
          {error}
          <button className="primary" disabled={working || code.length !== 6}>{working ? 'One moment…' : 'Continue'}</button>
        </form>
        <p className="hint">
          <button className="link" disabled={working} onClick={async () => { await sendCode(); setNote('We sent a new code. Use the newest one.'); }}>Send a new code</button>
          {' · '}
          <button className="link" onClick={back}>Use a different email</button>
        </p>
      </div>
    );
  }

  if (step === 'profile') {
    return (
      <div className="narrow">
        <h2>Welcome to {brand}</h2>
        <p className="hint">Your email <b>{sent.sentTo}</b> is confirmed. One last thing: what should we call you?</p>
        <form onSubmit={createAccount} className="checkout">
          <label>Name
            <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} autoComplete="name" autoFocus required />
          </label>
          <label>Mobile number (optional, for delivery updates)
            <PhoneField value={phone} onChange={setPhone} />
          </label>
          {!phoneIsEmpty(phone) && !phoneIsValid(phone) && <p className="field-error">Enter all 10 digits, or leave it empty.</p>}
          {error}
          <button className="primary" disabled={working || name.trim() === '' || (!phoneIsEmpty(phone) && !phoneIsValid(phone))}>
            {working ? 'One moment…' : 'Create my account'}
          </button>
        </form>
      </div>
    );
  }

  if (step === 'password') {
    return (
      <div className="narrow">
        <h2>Sign in with a password</h2>
        <p className="hint">For accounts made before email codes. Forgotten it? <button className="link" onClick={back}>Get an email code instead</button>.</p>
        <form onSubmit={withPassword} className="checkout">
          <label>Email or mobile number
            <input value={login} onChange={(e) => setLogin(e.target.value)} maxLength={254} autoComplete="username" required />
          </label>
          <label>Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} maxLength={72} autoComplete="current-password" required />
          </label>
          {error}
          <button className="primary" disabled={working || !login.trim() || !password}>{working ? 'One moment…' : 'Sign in'}</button>
        </form>
      </div>
    );
  }

  return (
    <div className="narrow">
      <h2>Sign in or create an account</h2>
      <p className="hint">Enter your email and we will send you a code. New here? The same code creates your account. One {brand} account works for Fresh and Fashion.</p>
      <form onSubmit={sendCode} className="checkout">
        <label>Email address
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={254}
            autoComplete="email" placeholder="you@gmail.com" autoFocus required />
        </label>
        {error}
        <button className="primary" disabled={working || !EMAIL.test(email.trim())}>{working ? 'Sending…' : 'Email me a code'}</button>
      </form>
      {passkeysSupported() && (
        <>
          <p className="or"><span>or</span></p>
          <button className="secondary" onClick={withPasskey} disabled={working}>Sign in with {unlock.button}</button>
          <p className="hint center">
            {unlock.elsewhere
              ? <>Use a passkey on {unlock.name}.</>
              : unlock.phone
                ? <>Uses {unlock.name}.</>
                : <>Uses {unlock.name} on this device, or your phone by scanning a QR code.</>}
            {' '}Set it up from your account page after signing in.
          </p>
        </>
      )}
      <p className="hint center"><button className="link" onClick={() => { setStep('password'); setProblem(null); }}>Use a password instead</button></p>
    </div>
  );
}
