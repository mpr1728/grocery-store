import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { money, POINT_REASONS } from '../format';
import PhoneField, { phoneIsEmpty, phoneIsValid } from '../PhoneField';
import { useStore } from '../store';

const dateTime = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });

export default function Account() {
  const { account, info, signedIn, signOut, refreshAccount } = useStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [mode, setMode] = useState('signin');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  // Sign-in accepts either the mobile number or the email address.
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  // Set while a sign-up waits for the code we emailed: { signupId, sentTo }.
  const [pending, setPending] = useState(null);
  const [code, setCode] = useState('');
  const [note, setNote] = useState(null);
  const [working, setWorking] = useState(false);
  const [problem, setProblem] = useState(null);

  // Points change when an order is delivered, so show the latest each time this page opens.
  useEffect(() => {
    refreshAccount();
  }, [refreshAccount]);

  // What the shopping assistant has kept about this customer. They can delete any of it.
  const [memory, setMemory] = useState([]);
  const signedInAs = account ? account.email || account.phone : null;
  useEffect(() => {
    if (!signedInAs) {
      setMemory([]);
      return;
    }
    api.assistantMemory().then(setMemory).catch(() => {});
  }, [signedInAs]);

  async function forget(factId) {
    try {
      if (factId === null) {
        await api.forgetAllMemory();
      } else {
        await api.forgetMemory(factId);
      }
      setMemory(await api.assistantMemory());
    } catch (e) {
      // The list simply stays as it was.
    }
  }

  const pointValue = info ? info.pointValuePaise : 100;

  if (account) {
    return (
      <div className="narrow">
        <h2>Hello, {account.name}</h2>
        <p className="unit">{[account.phone, account.email].filter(Boolean).join(' · ')}</p>

        <div className="points-card">
          <span className="points-number">{account.pointsBalance}</span>
          <span>reward points · worth {money(account.pointsBalance * pointValue)} at checkout</span>
        </div>
        {info && (
          <p className="hint">
            You earn {info.rewardEarnPercent}% of what you pay for items as points, added when the order is delivered.
            One point takes {money(pointValue)} off your next order.
          </p>
        )}

        <h3>Points history</h3>
        {account.recentPoints.length === 0 && <p className="hint">Nothing yet.</p>}
        {account.recentPoints.length > 0 && (
          <ul className="lines">
            {account.recentPoints.map((entry, index) => (
              <li key={index}>
                <div>
                  <strong>{POINT_REASONS[entry.reason] || entry.reason}</strong>
                  <span className="unit">
                    {dateTime.format(new Date(entry.createdAt))}
                    {entry.orderNumber && <> · <Link to={`/order/${entry.orderNumber}`}>{entry.orderNumber}</Link></>}
                  </span>
                </div>
                <span className={entry.points < 0 ? 'line-total spent' : 'line-total gained'}>
                  {entry.points > 0 ? '+' : '−'}{Math.abs(entry.points)}
                </span>
              </li>
            ))}
          </ul>
        )}

        <h3>What the shopping assistant remembers</h3>
        {memory.length === 0 && <p className="hint">Nothing yet. When you tell it things like your household size or diet, it keeps them here so you do not have to repeat them.</p>}
        {memory.length > 0 && (
          <>
            <ul className="lines">
              {memory.map((item) => (
                <li key={item.id}>
                  <div>{item.fact}</div>
                  <button className="link" onClick={() => forget(item.id)}>Delete</button>
                </li>
              ))}
            </ul>
            <p className="hint"><button className="link" onClick={() => forget(null)}>Forget everything</button></p>
          </>
        )}

        <p><button className="link" onClick={signOut}>Sign out</button></p>
      </div>
    );
  }

  const creating = mode === 'signup';
  // The mobile number is optional. If one is typed it has to be complete.
  const phoneOk = phoneIsEmpty(phone) || phoneIsValid(phone);
  const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  const passwordOk = creating ? password.length >= 8 : password.length > 0;
  const canSubmit = !working && passwordOk
    && (creating ? phoneOk && emailOk && name.trim() !== '' : login.trim() !== '');

  function finish(result) {
    signedIn(result);
    // Go back to where the customer came from, for example the cart.
    if (location.state && location.state.from) {
      navigate(location.state.from);
    }
  }

  function explain(e) {
    return e.code === 'VALIDATION_FAILED' ? 'Please check your details: ' + (e.details || []).join('; ') : e.message;
  }

  async function submit(event) {
    if (event) {
      event.preventDefault();
    }
    setWorking(true);
    setProblem(null);
    setNote(null);
    try {
      if (!creating) {
        finish(await api.signIn({ login: login.trim(), password }));
        return;
      }
      const result = await api.signUp({ name: name.trim(), phone: phoneIsEmpty(phone) ? null : phone, email: email.trim(), password });
      if (result.token) {
        finish(result);
      } else {
        // A code is on its way by email. The account is created once it is entered.
        setPending({ signupId: result.signupId, sentTo: result.sentTo });
        setCode('');
      }
    } catch (e) {
      setProblem(explain(e));
    } finally {
      setWorking(false);
    }
  }

  async function confirm(event) {
    event.preventDefault();
    setWorking(true);
    setProblem(null);
    setNote(null);
    try {
      finish(await api.confirmSignUp(pending.signupId, code));
    } catch (e) {
      setProblem(explain(e));
    } finally {
      setWorking(false);
    }
  }

  async function resend() {
    await submit();
    setNote('We sent a new code. Earlier codes no longer work with this page.');
  }

  if (pending) {
    return (
      <div className="narrow">
        <h2>Check your email</h2>
        <p className="hint">We sent a 6 digit code to <b>{pending.sentTo}</b>. It works for 10 minutes. Look in spam if you do not see it.</p>
        <form onSubmit={confirm} className="checkout">
          <label>Code
            <input className="code" value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" autoComplete="one-time-code" placeholder="6 digits" autoFocus required />
          </label>
          {note && <p className="hint">{note}</p>}
          {problem && <p className="notice error" role="alert">{problem}</p>}
          <button className="primary" disabled={working || code.length !== 6}>{working ? 'One moment…' : 'Confirm and create account'}</button>
        </form>
        <p className="hint">
          <button className="link" onClick={resend} disabled={working}>Send a new code</button>
          {' · '}
          <button className="link" onClick={() => { setPending(null); setProblem(null); setNote(null); }}>Change my details</button>
        </p>
      </div>
    );
  }

  return (
    <div className="narrow">
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={!creating} className={creating ? 'tab' : 'tab on'} onClick={() => { setMode('signin'); setProblem(null); }}>Sign in</button>
        <button role="tab" aria-selected={creating} className={creating ? 'tab on' : 'tab'} onClick={() => { setMode('signup'); setProblem(null); }}>Create account</button>
      </div>
      {creating && <p className="hint">An account earns reward points on every order and lets you use the shopping assistant.</p>}

      <form onSubmit={submit} className="checkout">
        {creating && (
          <label>Name
            <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} autoComplete="name" required />
          </label>
        )}
        {creating ? (
          <>
            <label>Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={254} autoComplete="email" placeholder="you@example.com" required />
            </label>
            {email !== '' && !emailOk && <p className="field-error">Enter a full email address.</p>}
            <label>Mobile number (optional)
              <PhoneField value={phone} onChange={setPhone} />
            </label>
            {!phoneOk && <p className="field-error">Enter all 10 digits, or leave it empty.</p>}
          </>
        ) : (
          <label>Email or mobile number
            <input value={login} onChange={(e) => setLogin(e.target.value)} maxLength={254} autoComplete="username" required />
          </label>
        )}
        {!creating && (
          <p className="hint">For a US number include the country code, for example +14155550123.</p>
        )}
        <label>Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} maxLength={72} autoComplete={creating ? 'new-password' : 'current-password'} required />
        </label>
        {creating && password !== '' && !passwordOk && <p className="field-error">Use at least 8 characters.</p>}
        {problem && <p className="notice error" role="alert">{problem}</p>}
        <button className="primary" disabled={!canSubmit}>
          {working ? 'One moment…' : creating ? 'Create account' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
