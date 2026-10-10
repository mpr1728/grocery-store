import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PhoneField, { phoneIsValid } from '../PhoneField';
import Stepper from '../Stepper';
import { api } from '../api';
import { money } from '../format';
import { useStore } from '../store';

const DETAILS_KEY = 'checkout.v1';

function loadDetails() {
  try {
    return JSON.parse(localStorage.getItem(DETAILS_KEY)) || {};
  } catch (e) {
    return {};
  }
}

export default function Cart() {
  const { hub, info, lines, itemTotalPaise, deliveryFeePaise, totalPaise, setQuantity, clearCart, reload, loading, account, refreshAccount } = useStore();
  const navigate = useNavigate();
  const saved = loadDetails();
  const [typedName, setName] = useState(saved.name || '');
  const [typedPhone, setPhone] = useState(saved.phone || '');
  const [usePoints, setUsePoints] = useState(false);
  const [phoneEdited, setPhoneEdited] = useState(false);
  const [address, setAddress] = useState(saved.address || '');
  const [placing, setPlacing] = useState(false);
  const [problem, setProblem] = useState(null);
  const [shortages, setShortages] = useState([]);

  if (loading) {
    return <p className="notice">Loading your cart…</p>;
  }
  if (lines.length === 0) {
    return <p className="notice">Your cart is empty. <Link to="/">Go to the shop</Link></p>;
  }

  // A signed-in customer orders under their own account. The number is the one to call for this
  // delivery: it starts as the account's number, if it has one, and can be changed.
  const name = account ? account.name : typedName;
  const phone = phoneEdited || typedPhone !== '' ? typedPhone : (account && account.phone) || '';
  const phoneOk = phoneIsValid(phone);

  // Points take money off the items only. The server applies the same rules.
  const pointValue = info ? info.pointValuePaise : 100;
  const usablePoints = account ? Math.min(account.pointsBalance, Math.floor(itemTotalPaise / pointValue)) : 0;
  const pointsToRedeem = usePoints ? usablePoints : 0;
  const discountPaise = pointsToRedeem * pointValue;
  const toPayPaise = totalPaise - discountPaise;
  const willEarn = account && info ? Math.floor((itemTotalPaise - discountPaise) * info.rewardEarnPercent / 100 / pointValue) : 0;
  const canPlace = phoneOk && address.trim() !== '' && !placing;
  const toFreeDelivery = info && deliveryFeePaise > 0 ? info.freeDeliveryAbovePaise - itemTotalPaise : 0;

  async function placeOrder(event) {
    event.preventDefault();
    setPlacing(true);
    setProblem(null);
    setShortages([]);
    try {
      localStorage.setItem(DETAILS_KEY, JSON.stringify({ name: typedName, phone, address }));
    } catch (e) {
      // Saving details is a convenience only.
    }
    try {
      const order = await api.placeOrder({
        hubId: hub.id,
        phone,
        customerName: name,
        deliveryAddress: address,
        lines: lines.map((line) => ({ productId: line.item.productId, quantity: line.quantity })),
        pointsToRedeem
      });
      clearCart();
      reload();
      refreshAccount();
      navigate(`/order/${order.orderNumber}`);
    } catch (e) {
      if (e.code === 'INSUFFICIENT_STOCK') {
        setShortages(e.details || []);
        setProblem('Some items sold faster than expected. We have adjusted your cart to what is left. Please review it and place the order again.');
        (e.details || []).forEach((s) => setQuantity(s.productId, s.available));
        reload();
      } else if (e.code === 'VALIDATION_FAILED') {
        setProblem('Please check your details: ' + (e.details || []).join('; '));
      } else {
        setProblem(e.message);
      }
    } finally {
      setPlacing(false);
    }
  }

  return (
    <div className="two-col">
      <section>
        <div className="cart-head">
          <h2>Your cart</h2>
          <button className="link" onClick={() => { if (window.confirm('Remove everything from your cart?')) clearCart(); }}>Clear cart</button>
        </div>
        {account && <p className="hint">Saved with your account, so it will be here when you come back.</p>}
        <ul className="lines">
          {lines.map(({ item, quantity }) => (
            <li key={item.productId}>
              <div>
                <strong>{item.name}</strong>
                <span className="unit">{item.unitLabel} · {money(item.pricePaise)}</span>
              </div>
              <Stepper item={item} />
              <span className="line-total">{money(item.pricePaise * quantity)}</span>
            </li>
          ))}
        </ul>
        {shortages.length > 0 && (
          <ul className="shortages">
            {shortages.map((s) => (
              <li key={s.productId}>{s.productName}: you asked for {s.requested}, {s.available === 0 ? 'none left' : `${s.available} left`}</li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2>Bill</h2>
        <dl className="bill">
          <dt>Items</dt><dd>{money(itemTotalPaise)}</dd>
          <dt>Delivery</dt><dd>{deliveryFeePaise === 0 ? 'Free' : money(deliveryFeePaise)}</dd>
          {discountPaise > 0 && <><dt>Reward points ({pointsToRedeem})</dt><dd>−{money(discountPaise)}</dd></>}
          <dt className="total">To pay</dt><dd className="total">{money(toPayPaise)}</dd>
        </dl>
        {account && usablePoints > 0 && (
          <label className="use-points">
            <input type="checkbox" checked={usePoints} onChange={(e) => setUsePoints(e.target.checked)} />
            <span>Use {usablePoints} reward points <b>({money(usablePoints * pointValue)} off)</b></span>
          </label>
        )}
        {account && willEarn > 0 && <p className="earn">You earn {willEarn} reward points when this order is delivered.</p>}
        {!account && <p className="earn"><Link to="/account" state={{ from: '/cart' }}>Sign in or create an account</Link> to earn reward points on this order.</p>}
        {toFreeDelivery > 0 && <p className="hint">Add {money(toFreeDelivery)} more for free delivery.</p>}
        <p className="hint">This is the full amount. No other fees are added later.</p>

        <form onSubmit={placeOrder} className="checkout">
          <label>Name
            <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} autoComplete="name" readOnly={!!account} />
          </label>
          <label>Mobile number for this delivery
            <PhoneField value={phone} onChange={(value) => { setPhoneEdited(true); setPhone(value); }} required />
          </label>
          {phone !== '' && !phoneOk && <p className="field-error">Enter all 10 digits of the mobile number.</p>}
          <label>Delivery address
            <textarea value={address} onChange={(e) => setAddress(e.target.value)} maxLength={300} rows={3} placeholder="Flat, tower, community" required />
          </label>
          {problem && <p className="notice error" role="alert">{problem}</p>}
          <button className="primary" disabled={!canPlace}>{placing ? 'Placing your order…' : `Place order · ${money(toPayPaise)}`}</button>
          <p className="hint">Pay on delivery for now. Online payment is not connected yet.</p>
        </form>
      </section>
    </div>
  );
}
