import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { BRAND } from '../brand';
import Passkeys from '../Passkeys';
import SignIn from '../SignIn';
import { useFashion } from '../fashion';

export default function Account() {
  const { account, signedIn, signOut } = useFashion();
  const navigate = useNavigate();
  const location = useLocation();

  if (!account) {
    return (
      <SignIn brand={BRAND} onSignedIn={(result) => {
        signedIn(result);
        // Go back to where the customer came from, for example the review step.
        if (location.state && location.state.from) {
          navigate(location.state.from);
        }
      }} />
    );
  }

  return (
    <div className="narrow">
      <h2>Hello, {account.name}</h2>
      <p className="unit">{[account.email, account.phone].filter(Boolean).join(' · ')}</p>
      <p className="hint">The same account works in {BRAND} Fresh, with your reward points there.</p>
      <ul className="lines">
        <li><Link to="/orders">Your orders</Link></li>
        <li><Link to="/fit">Your measurements</Link></li>
      </ul>
      <Passkeys />
      <p><button className="link" onClick={signOut}>Sign out</button></p>
    </div>
  );
}
