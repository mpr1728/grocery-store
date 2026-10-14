import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { dateTime, money, STATUS_LABELS } from '../format';
import { useFashion } from '../fashion';

export default function Orders() {
  const { account } = useFashion();
  const [orders, setOrders] = useState(null);
  const [problem, setProblem] = useState(null);

  useEffect(() => {
    if (account) {
      api.myOrders().then(setOrders).catch((e) => setProblem(e.message));
    }
  }, [account]);

  if (!account) {
    return <p className="notice"><Link to="/account" state={{ from: '/orders' }}>Sign in</Link> to see your orders.</p>;
  }
  if (problem) {
    return <p className="notice error">{problem}</p>;
  }
  if (!orders) {
    return <p className="hint">Loading…</p>;
  }
  return (
    <div className="narrow">
      <h2>Your orders</h2>
      {orders.length === 0 && <p className="notice">No orders yet. <Link to="/design">Design your first garment</Link></p>}
      {orders.length > 0 && (
        <ul className="lines">
          {orders.map((o) => (
            <li key={o.orderNumber}>
              <span className="dot big" style={{ background: o.colourHex }} />
              <div>
                <Link to={`/order/${o.orderNumber}`}><strong>{o.garmentName}</strong> · {o.orderNumber}</Link>
                <span className="unit">{dateTime.format(new Date(o.placedAt))} · {STATUS_LABELS[o.status]}</span>
              </div>
              <span className="line-total">{money(o.totalPaise)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
