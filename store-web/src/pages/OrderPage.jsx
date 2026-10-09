import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { money, STATUS_LABELS, STATUS_ORDER } from '../format';

export default function OrderPage() {
  const { orderNumber } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let stopped = false;
    let timer;
    async function load() {
      try {
        const latest = await api.order(orderNumber);
        if (stopped) return;
        setOrder(latest);
        setError(null);
        if (latest.status !== 'DELIVERED') {
          timer = setTimeout(load, 10000);
        }
      } catch (e) {
        if (stopped) return;
        setError(e.message);
        if (e.status !== 404) {
          timer = setTimeout(load, 10000);
        }
      }
    }
    load();
    return () => {
      stopped = true;
      clearTimeout(timer);
    };
  }, [orderNumber]);

  if (!order) {
    return <p className={error ? 'notice error' : 'notice'}>{error || 'Loading your order…'}</p>;
  }

  const step = STATUS_ORDER.indexOf(order.status);
  return (
    <div className="narrow">
      <h2>{order.status === 'DELIVERED' ? 'Delivered' : 'Thank you. Your order is in.'}</h2>
      <p className="unit">Order {order.orderNumber}</p>

      <ol className="tracker">
        {STATUS_ORDER.map((status, index) => (
          <li key={status} className={index < step ? 'done' : index === step ? 'current' : ''}>{STATUS_LABELS[status]}</li>
        ))}
      </ol>

      <ul className="lines">
        {order.lines.map((line) => (
          <li key={line.productId}>
            <div>
              <strong>{line.productName}</strong>
              <span className="unit">{line.unitLabel} × {line.quantity}</span>
            </div>
            <span className="line-total">{money(line.lineTotalPaise)}</span>
          </li>
        ))}
      </ul>
      <dl className="bill">
        <dt>Items</dt><dd>{money(order.itemTotalPaise)}</dd>
        <dt>Delivery</dt><dd>{order.deliveryFeePaise === 0 ? 'Free' : money(order.deliveryFeePaise)}</dd>
        {order.pointsDiscountPaise > 0 && <><dt>Reward points ({order.pointsRedeemed})</dt><dd>−{money(order.pointsDiscountPaise)}</dd></>}
        <dt className="total">Total</dt><dd className="total">{money(order.totalPaise)}</dd>
      </dl>
      {order.pointsEarned > 0 && (
        <p className="earn">
          {order.status === 'DELIVERED'
            ? `${order.pointsEarned} reward points were added to your account.`
            : `You earn ${order.pointsEarned} reward points when this order is delivered.`}
        </p>
      )}
      <p className="unit">Delivering to: {order.deliveryAddress}</p>
      {error && <p className="hint">Could not refresh the status just now. Trying again shortly.</p>}
      <p><Link to="/">Continue shopping</Link></p>
    </div>
  );
}
