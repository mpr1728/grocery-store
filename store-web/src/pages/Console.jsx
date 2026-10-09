import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { money, STATUS_LABELS } from '../format';
import { useStore } from '../store';
import { BatchTest, GrowingTab, ImportTab } from './ConsoleCatalog';

const NEXT_ACTION = {
  PLACED: 'Start picking',
  PICKING: 'Mark packed',
  PACKED: 'Hand to rider',
  OUT_FOR_DELIVERY: 'Mark delivered'
};

export default function Console() {
  const { hub, catalog, loading, error: storeError, reload: reloadStore } = useStore();
  const [tab, setTab] = useState('orders');
  const [orders, setOrders] = useState([]);
  const [stock, setStock] = useState([]);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(null);
  const [receipt, setReceipt] = useState({ productId: '', quantity: '', useBy: '', bin: '' });
  const [receiptNote, setReceiptNote] = useState(null);

  const refresh = useCallback(async () => {
    if (!hub) return;
    try {
      const [latestOrders, latestStock] = await Promise.all([api.hubOrders(hub.id), api.stock(hub.id)]);
      setOrders(latestOrders);
      setStock(latestStock);
      setError(null);
    } catch (e) {
      setError(e.message);
    }
  }, [hub]);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 15000);
    return () => clearInterval(timer);
  }, [refresh]);

  if (loading) return <p className="notice">Loading the hub console…</p>;
  if (storeError) return <p className="notice error">{storeError}</p>;

  async function advance(orderNumber) {
    setBusy(orderNumber);
    try {
      await api.advanceOrder(orderNumber);
    } catch (e) {
      setError(e.message);
    }
    await refresh();
    setBusy(null);
  }

  async function receive(event) {
    event.preventDefault();
    setReceiptNote(null);
    try {
      const saved = await api.receiveStock(hub.id, {
        productId: Number(receipt.productId),
        quantity: Number(receipt.quantity),
        useBy: receipt.useBy || null,
        bin: receipt.bin || null
      });
      setReceiptNote({ ok: true, text: `Received ${saved.quantity} units as batch ${saved.batchCode}${saved.useBy ? `, use by ${saved.useBy}` : ''}.` });
      setReceipt({ productId: '', quantity: '', useBy: '', bin: '' });
      refresh();
      reloadStore();
    } catch (e) {
      setReceiptNote({ ok: false, text: e.details ? `${e.message}: ${e.details.join('; ')}` : e.message });
    }
  }

  const open = orders.filter((o) => o.status !== 'DELIVERED');
  return (
    <div>
      <h2>{hub.name} console</h2>
      <p className="hint">Staff screen. It has no sign-in of its own yet, so anyone who can open the site can use it.</p>
      <div className="chips">
        <button className={tab === 'orders' ? 'chip on' : 'chip'} onClick={() => setTab('orders')}>Orders ({open.length} open)</button>
        <button className={tab === 'stock' ? 'chip on' : 'chip'} onClick={() => setTab('stock')}>Stock</button>
        <button className={tab === 'growing' ? 'chip on' : 'chip'} onClick={() => setTab('growing')}>How it was grown</button>
        <button className={tab === 'import' ? 'chip on' : 'chip'} onClick={() => setTab('import')}>Import review</button>
      </div>
      {error && <p className="notice error" role="alert">{error}</p>}

      {tab === 'orders' && (
        orders.length === 0 ? <p className="notice">No orders yet.</p> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Order</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.orderNumber}>
                    <td>{o.orderNumber}</td>
                    <td>{o.customerName || 'No name'}<br /><span className="unit">{o.phone}<br />{o.deliveryAddress}</span></td>
                    <td>{o.lines.map((l) => <div key={l.productId}>{l.quantity} × {l.productName} <span className="unit">{l.unitLabel}</span></div>)}</td>
                    <td>{money(o.totalPaise)}</td>
                    <td>{STATUS_LABELS[o.status]}</td>
                    <td>{NEXT_ACTION[o.status] && <button className="small" disabled={busy === o.orderNumber} onClick={() => advance(o.orderNumber)}>{NEXT_ACTION[o.status]}</button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {tab === 'stock' && (
        <>
          <form className="receipt" onSubmit={receive}>
            <strong>Receive stock</strong>
            <select aria-label="Product" value={receipt.productId} onChange={(e) => setReceipt({ ...receipt, productId: e.target.value })} required>
              <option value="">Choose a product</option>
              {catalog.map((item) => <option key={item.productId} value={item.productId}>{item.name} ({item.unitLabel})</option>)}
            </select>
            <input aria-label="Quantity" type="number" min="1" placeholder="Quantity" value={receipt.quantity} onChange={(e) => setReceipt({ ...receipt, quantity: e.target.value })} required />
            <input aria-label="Use by date" type="date" value={receipt.useBy} onChange={(e) => setReceipt({ ...receipt, useBy: e.target.value })} />
            <input aria-label="Bin" placeholder="Bin" maxLength={20} value={receipt.bin} onChange={(e) => setReceipt({ ...receipt, bin: e.target.value })} />
            <button className="small">Receive</button>
          </form>
          <p className="hint">Leave the date empty to use the product's normal shelf life.</p>
          {receiptNote && <p className={receiptNote.ok ? 'notice' : 'notice error'} role="status">{receiptNote.text}</p>}
          <div className="table-wrap">
            <table>
              <thead><tr><th>Product</th><th>Batch</th><th>Bin</th><th>Quantity</th><th>Use by</th><th>Residue test</th></tr></thead>
              <tbody>
                {stock.map((s) => (
                  <tr key={s.batchId}>
                    <td>{s.productName} <span className="unit">{s.unitLabel}</span></td>
                    <td>{s.batchCode}</td>
                    <td>{s.bin}</td>
                    <td>{s.quantity}</td>
                    <td>{s.useBy == null ? <span className="unit">No date</span> : (
                      <span className={s.daysLeft < 0 ? 'badge bad' : s.daysLeft <= 1 ? 'badge warn' : 'badge'}>
                        {s.useBy} · {s.daysLeft < 0 ? 'expired' : s.daysLeft === 0 ? 'today' : `${s.daysLeft} d`}
                      </span>
                    )}</td>
                    <td><BatchTest hubId={hub.id} batch={s} onSaved={() => { refresh(); reloadStore(); }} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'growing' && <GrowingTab hubId={hub.id} catalog={catalog} onSaved={reloadStore} />}
      {tab === 'import' && <ImportTab hubId={hub.id} onChanged={reloadStore} />}
    </div>
  );
}
