import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { calendarDay, dateTime, MEASUREMENTS, money, STATUS_LABELS, STATUS_ORDER } from '../format';
import GarmentSketch from '../GarmentSketch';
import { useFashion } from '../fashion';

/** Turns the stored codes (garment/neckline/sleeve/length/fit/fabric/colour) back into a design for the sketch. */
export function designOf(order) {
  const [garment, neckline, sleeve, length, fit, fabric, colour] = order.designCodes.split('/');
  return { garment, neckline, sleeve, length, fit, fabric, colour };
}

export default function OrderPage() {
  const { orderNumber } = useParams();
  const { account } = useFashion();
  const [order, setOrder] = useState(null);
  const [problem, setProblem] = useState(null);
  const [working, setWorking] = useState(false);

  useEffect(() => {
    if (account) {
      api.order(orderNumber).then(setOrder).catch((e) => setProblem(e.message));
    }
  }, [orderNumber, account]);

  if (!account) {
    return <p className="notice"><Link to="/account" state={{ from: `/order/${orderNumber}` }}>Sign in</Link> to see this order.</p>;
  }
  if (problem && !order) {
    return <p className="notice error">{problem}</p>;
  }
  if (!order) {
    return <p className="hint">Loading…</p>;
  }

  async function cancel() {
    setWorking(true);
    setProblem(null);
    try {
      setOrder(await api.cancelOrder(order.orderNumber));
    } catch (e) {
      setProblem(e.message);
    } finally {
      setWorking(false);
    }
  }

  const reached = STATUS_ORDER.indexOf(order.status);
  return (
    <div className="two-col">
      <div>
        <h2>Order {order.orderNumber}</h2>
        <p className="unit">Placed {dateTime.format(new Date(order.placedAt))}</p>
        {order.status === 'CANCELLED' ? (
          <p className="notice">This order was cancelled. Nothing is charged.</p>
        ) : (
          <>
            <ol className="tracker">
              {STATUS_ORDER.map((status, index) => (
                <li key={status} className={index < reached ? 'done' : index === reached ? 'done current' : ''}>
                  {STATUS_LABELS[status]}
                </li>
              ))}
            </ol>
            {order.status !== 'DELIVERED' && <p className="hint">Expected to be sent by {calendarDay(order.readyBy)}.</p>}
          </>
        )}
        {order.canCancel && (
          <p>
            <button className="link danger" onClick={cancel} disabled={working}>Cancel this order</button>
            <span className="hint"> · possible until the fabric is cut</span>
          </p>
        )}
        {problem && <p className="notice error" role="alert">{problem}</p>}

        <dl className="bill">
          <dt>{order.garmentName}</dt><dd>{money(order.basePricePaise)}</dd>
          {order.extrasPaise > 0 && <><dt>Design and fabric extras</dt><dd>{money(order.extrasPaise)}</dd></>}
          <dt>Shipping</dt><dd>{order.shippingPaise === 0 ? 'Free' : money(order.shippingPaise)}</dd>
          <dt className="total">Total, pay on delivery</dt><dd className="total">{money(order.totalPaise)}</dd>
        </dl>
        <p className="hint">Delivering to {order.deliveryAddress} · {order.phone}</p>
      </div>

      <div className="review-design">
        <GarmentSketch design={designOf(order)} colourHex={order.colourHex} size={180} />
        <div>
          <dl className="facts">
            {order.choices.map((c) => <React.Fragment key={c.part}><dt>{c.part}</dt><dd>{c.value}</dd></React.Fragment>)}
          </dl>
          <h3>Cut to</h3>
          <dl className="facts">
            {MEASUREMENTS.map((m) => <React.Fragment key={m.key}><dt>{m.label}</dt><dd>{order.measurements[m.key]} cm</dd></React.Fragment>)}
          </dl>
          {order.notes && <p className="hint">Your note: {order.notes}</p>}
        </div>
      </div>
    </div>
  );
}
