import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { money, POINT_REASONS } from '../format';
import Passkeys from '../Passkeys';
import SignIn from '../SignIn';
import { BRAND } from '../brand';
import { useStore } from '../store';

const dateTime = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });

export default function Account() {
  const { account, info, signedIn, signOut, refreshAccount } = useStore();
  const navigate = useNavigate();
  const location = useLocation();
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

        <Passkeys />

        <p><button className="link" onClick={signOut}>Sign out</button></p>
      </div>
    );
  }

  return (
    <SignIn brand={BRAND} onSignedIn={(result) => {
      signedIn(result);
      // Go back to where the customer came from, for example the cart.
      if (location.state && location.state.from) {
        navigate(location.state.from);
      }
    }} />
  );
}
