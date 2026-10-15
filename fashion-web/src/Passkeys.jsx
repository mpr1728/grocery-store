import React, { useEffect, useState } from 'react';
import { api } from './api';
import { createPasskey, passkeyLabel, passkeysSupported, useUnlockMethod, wasCancelled } from './webauthn';

const day = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });

/** The signed-in customer's passkeys, with add and remove. */
export default function Passkeys() {
  const [list, setList] = useState(null);
  const [working, setWorking] = useState(false);
  const [problem, setProblem] = useState(null);
  const [note, setNote] = useState(null);
  const unlock = useUnlockMethod();

  const load = () => api.passkeys().then(setList).catch(() => setList([]));
  useEffect(() => { load(); }, []);

  async function add() {
    setWorking(true);
    setProblem(null);
    setNote(null);
    try {
      const options = await api.passkeyOptions();
      const answer = await createPasskey(options.optionsJson);
      await api.addPasskey(options.ceremonyId, answer, passkeyLabel(answer));
      setNote(`Done. Next time, choose "Sign in with ${unlock.button}".`);
      load();
    } catch (e) {
      if (!wasCancelled(e)) {
        setProblem(e.status !== undefined ? e.message : 'The passkey could not be made. Try again, or choose another device in the prompt.');
      }
    } finally {
      setWorking(false);
    }
  }

  async function remove(id) {
    try {
      await api.removePasskey(id);
    } finally {
      load();
    }
  }

  return (
    <>
      <h3>Passkeys</h3>
      <p className="hint">
        {unlock.elsewhere
          ? <>This device has no built-in unlock set up, so keep your passkey on {unlock.name}.</>
          : <>Sign in with {unlock.name} instead of an email code.</>}
        {' '}When you add one, your browser also offers a password manager (iCloud Keychain, Google Password Manager,
        1Password, and others), another phone by QR code, or a security key. Passkeys in iCloud or Google sync to your
        other devices on the same account.
      </p>
      {list && list.length > 0 && (
        <ul className="lines">
          {list.map((p) => (
            <li key={p.id}>
              <div>
                <strong>{p.label}</strong>
                <span className="unit">Added {day.format(new Date(p.createdAt))}{p.lastUsedAt && ` · last used ${day.format(new Date(p.lastUsedAt))}`}</span>
              </div>
              <button className="link" onClick={() => remove(p.id)}>Remove</button>
            </li>
          ))}
        </ul>
      )}
      {note && <p className="hint">{note}</p>}
      {problem && <p className="notice error" role="alert">{problem}</p>}
      {passkeysSupported()
        ? <p><button className="secondary" onClick={add} disabled={working}>{working ? 'Waiting for your passkey…' : unlock.elsewhere ? 'Add a passkey' : `Set up ${unlock.button} sign-in`}</button></p>
        : <p className="hint">This browser does not support passkeys.</p>}
    </>
  );
}
