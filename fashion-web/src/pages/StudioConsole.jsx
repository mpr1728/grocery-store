import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { calendarDay, MEASUREMENTS, money, STATUS_LABELS, STATUS_ORDER } from '../format';
import GarmentSketch from '../GarmentSketch';
import { designOf } from './OrderPage';

/** For the tailoring partner: open orders with everything needed to cut them, and a button for the next stage. */
export default function StudioConsole() {
  const [orders, setOrders] = useState(null);
  const [problem, setProblem] = useState(null);
  const [busy, setBusy] = useState(null);

  const load = useCallback(() => {
    api.studioOrders().then(setOrders).catch((e) => setProblem(e.message));
  }, []);

  useEffect(load, [load]);

  async function advance(orderNumber) {
    setBusy(orderNumber);
    setProblem(null);
    try {
      await api.advanceOrder(orderNumber);
      load();
    } catch (e) {
      setProblem(e.message);
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <div className="cart-head">
        <h2>Studio orders</h2>
        <button className="link" onClick={load}>Refresh</button>
      </div>
      <p className="hint">Orders still to cut, stitch, check, or deliver. Customers can cancel until you mark the fabric cut.</p>
      {problem && <p className="notice error" role="alert">{problem}</p>}
      {orders && orders.length === 0 && <p className="notice">No open orders.</p>}
      <div className="studio-orders">
        {(orders || []).map((o) => {
          const next = STATUS_ORDER[STATUS_ORDER.indexOf(o.status) + 1];
          return (
            <article key={o.orderNumber} className="studio-order">
              <GarmentSketch design={designOf(o)} colourHex={o.colourHex} size={90} />
              <div>
                <h3>{o.garmentName} · {o.orderNumber} <span className="badge">{STATUS_LABELS[o.status]}</span></h3>
                <p className="unit">{o.choices.map((c) => `${c.part}: ${c.value}`).join(' · ')}</p>
                <p className="unit">
                  {MEASUREMENTS.map((m) => `${m.label} ${o.measurements[m.key]}`).join(' · ')} cm · reads as {o.shapeLabel}
                </p>
                {o.notes && <p className="unit">Note: {o.notes}</p>}
                <p className="unit">{o.customerName} · {o.phone} · {o.deliveryAddress} · {money(o.totalPaise)} · send by {calendarDay(o.readyBy)}</p>
              </div>
              {next && (
                <button className="small" disabled={busy === o.orderNumber} onClick={() => advance(o.orderNumber)}>
                  Mark: {STATUS_LABELS[next]}
                </button>
              )}
            </article>
          );
        })}
      </div>
    </>
  );
}
