import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { money } from '../format';
import { MAX_PER_ITEM, useStore } from '../store';

const EXAMPLES = ['Palak paneer for 4', 'Weekly groceries for two under 1500 rupees', 'A week of vegetarian food with 70 g protein a day'];
const HISTORY_TURNS = 10;

const NUTRIENTS = [
  ['proteinG', 'Protein', 'g'],
  ['carbsG', 'Carbs', 'g'],
  ['fatG', 'Fat', 'g'],
  ['fiberG', 'Fibre', 'g'],
  ['kcal', 'Energy', 'kcal']
];

/** What the suggested list contains, added up by the store from pack weights, next to any target given. */
function Nutrition({ summary }) {
  const daily = summary.perPersonPerDay;
  const shown = daily || summary.total;
  const target = summary.target || {};
  return (
    <div className="nutrition">
      <strong>
        {daily
          ? `Per person per day (${summary.people} ${summary.people === 1 ? 'person' : 'people'}, ${summary.days} ${summary.days === 1 ? 'day' : 'days'})`
          : 'In this whole list'}
      </strong>
      <dl>
        {NUTRIENTS.map(([key, label, unit]) => (
          <div key={key}>
            <dt>{label}</dt>
            <dd>
              {shown[key].toLocaleString('en-IN')} {unit}
              {daily && target[key] != null && <span className="unit">target {target[key].toLocaleString('en-IN')}</span>}
            </dd>
          </div>
        ))}
      </dl>
      <p className="hint">
        Worked out from pack weights and per 100 g figures for the food as sold, before cooking. Approximate.
        {summary.itemsNotCounted > 0 && ` ${summary.itemsNotCounted} ${summary.itemsNotCounted === 1 ? 'item has' : 'items have'} no figures and ${summary.itemsNotCounted === 1 ? 'is' : 'are'} not counted.`}
      </p>
    </div>
  );
}

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
    // Each earlier answer goes back with the list it showed, so a follow-up changes that list.
    const history = turns.slice(-HISTORY_TURNS).map((turn) => ({
      role: turn.role,
      text: turn.text.slice(0, 1000),
      items: turn.items ? turn.items.map((item) => ({ productId: item.productId, quantity: item.quantity })) : null
    }));
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
        nutrition: answer.nutrition,
        notes: answer.notes,
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
      <p className="hint">Tell me what you want to cook or what you need. I will pick the items from this store for you. You can then ask me to change the list, for example "leave out the oil" or "keep it under 800 rupees".</p>

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
            {turn.notes && turn.notes.map((note) => <p key={note} className="hint">{note}</p>)}
            {turn.nutrition && <Nutrition summary={turn.nutrition} />}
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
