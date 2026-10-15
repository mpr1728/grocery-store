import React, { useEffect, useState } from 'react';
import { api } from './api';
import { createPasskey, deviceLabel, passkeysSupported, wasCancelled } from './webauthn';

const day = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });

/** The signed-in customer's passkeys, with add and remove. */
export default function Passkeys() {
  const [list, setList] = useState(null);
  const [working, setWorking] = useState(false);
  const [problem, setProblem] = useState(null);
  const [note, setNote] = useState(null);

  const load = () => api.passkeys().then(setList).catch(() => setList([]));
  useEffect(() => { load(); }, []);

  async function add() {
    setWorking(true);
    setProblem(null);
    setNote(null);
    try {
      const options = await api.passkeyOptions();
      const answer = await createPasskey(options.optionsJson);
      await api.addPasskey(options.ceremonyId, answer, deviceLabel());
      setNote('Passkey added. Next time, choose "Sign in with a passkey".');
      load();
    } catch (e) {
      if (!wasCancelled(e)) {
        setProblem(e.status !== undefined ? e.message : 'This device could not make a passkey here.');
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
      <p className="hint">Sign in with Face ID, Touch ID, or your screen lock instead of an email code. Passkeys sync through your Apple, Google, or Microsoft account to your other devices.</p>
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
        ? <p><button className="secondary" onClick={add} disabled={working}>{working ? 'Waiting for your device…' : 'Add a passkey on this device'}</button></p>
        : <p className="hint">This browser does not support passkeys.</p>}
    </>
  );
}
