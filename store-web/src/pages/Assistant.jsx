import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { money } from '../format';
import { MAX_PER_ITEM, useStore } from '../store';

const EXAMPLES = ['Palak paneer for 4', 'Idli and sambar breakfast for 6', 'Basics for a week for two people'];
const HISTORY_TURNS = 10;

export default function Assistant() {
  const { hub, info, catalog, cart, setQuantity, account, loading } = useStore();
  // Each turn is { role: 'user' | 'assistant', text, items, notStocked, added }.
  const [turns, setTurns] = useState([]);
  const [message, setMessage] = useState('');
  const [thinking, setThinking] = useState(false);
  const [problem, setProblem] = useState(null);
  const bottom = useRef(null);

  useEffect(() => {
    if (bottom.current) {
      bottom.current.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }
  }, [turns, thinking]);

  if (loading) {
    return <p className="notice">Loading…</p>;
  }
  if (!info || !info.assistantEnabled) {
    return <p className="notice">The shopping assistant is not available right now. <Link to="/">Back to the shop</Link></p>;
  }
  if (!account) {
    return (
      <p className="notice">
        <Link to="/account" state={{ from: '/assistant' }}>Sign in or create an account</Link> to use the shopping assistant.
      </p>
    );
  }

  async function ask(text) {
    const question = text.trim();
    if (question === '' || thinking) {
      return;
    }
    const history = turns.slice(-HISTORY_TURNS).map((turn) => ({ role: turn.role, text: turn.text.slice(0, 1000) }));
    setTurns((current) => [...current, { role: 'user', text: question }]);
    setMessage('');
    setThinking(true);
    setProblem(null);
    try {
      const answer = await api.askAssistant(hub.id, question, history);
      setTurns((current) => [...current, {
        role: 'assistant',
        text: answer.reply || 'Here is what I found.',
        items: answer.items,
        notStocked: answer.notStocked,
        added: false
      }]);
    } catch (e) {
      setProblem(e.message);
    } finally {
      setThinking(false);
    }
  }

  function addToCart(index) {
    const available = new Map(catalog.map((item) => [item.productId, item.available]));
    turns[index].items.forEach((item) => {
      const limit = Math.min(available.get(item.productId) || 0, MAX_PER_ITEM);
      const wanted = (cart[item.productId] || 0) + item.quantity;
      if (limit > 0) {
        setQuantity(item.productId, Math.min(wanted, limit));
      }
    });
    setTurns((current) => current.map((turn, i) => (i === index ? { ...turn, added: true } : turn)));
  }

  return (
    <div className="narrow chat">
      <h2>Shopping assistant</h2>
      <p className="hint">Tell me what you want to cook or what you need. I will pick the items from this store for you.</p>

      {turns.length === 0 && (
        <div className="chips">
          {EXAMPLES.map((example) => (
            <button key={example} className="chip" onClick={() => ask(example)}>{example}</button>
          ))}
        </div>
      )}

      <div className="turns" aria-live="polite">
        {turns.map((turn, index) => (
          <div key={index} className={turn.role === 'user' ? 'bubble mine' : 'bubble'}>
            <p>{turn.text}</p>
            {turn.items && turn.items.length > 0 && (
              <>
                <ul className="lines">
                  {turn.items.map((item) => (
                    <li key={item.productId}>
                      <div>
                        <strong>{item.name}</strong>
                        <span className="unit">{item.unitLabel} × {item.quantity}{item.note && ` · ${item.note}`}</span>
                      </div>
                      <span className="line-total">{money(item.pricePaise * item.quantity)}</span>
                    </li>
                  ))}
                </ul>
                <p className="suggest-total">
                  Total for these: <b>{money(turn.items.reduce((sum, item) => sum + item.pricePaise * item.quantity, 0))}</b>
                </p>
                {turn.added
                  ? <p className="earn">Added. <Link to="/cart">Review your cart</Link></p>
                  : <button className="small" onClick={() => addToCart(index)}>Add these to my cart</button>}
              </>
            )}
            {turn.notStocked && turn.notStocked.length > 0 && (
              <p className="hint">Not available here: {turn.notStocked.join(', ')}.</p>
            )}
          </div>
        ))}
        {thinking && <div className="bubble"><p className="hint">Putting your list together…</p></div>}
        <div ref={bottom} />
      </div>

      {problem && <p className="notice error" role="alert">{problem}</p>}

      <form className="ask" onSubmit={(e) => { e.preventDefault(); ask(message); }}>
        <input
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          maxLength={500}
          placeholder="For example: dosa for 5 with chutney"
          aria-label="What do you need?"
        />
        <button className="small" disabled={thinking || message.trim() === ''}>Send</button>
      </form>
      <p className="hint">Suggestions come from an AI model and can be wrong. Check the list before you order.</p>
    </div>
  );
}
