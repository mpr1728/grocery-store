import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { calendarDay, MEASUREMENTS, money } from '../format';
import GarmentSketch from '../GarmentSketch';
import PhoneField, { phoneIsValid } from '../PhoneField';
import Steps from '../Steps';
import { useFashion } from '../fashion';

export default function Review() {
  const { catalog, design, setDesign, fit, account } = useFashion();
  const navigate = useNavigate();
  const [quote, setQuote] = useState(null);
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [notes, setNotes] = useState('');
  const [accepted, setAccepted] = useState(false);
  const [working, setWorking] = useState(false);
  const [problem, setProblem] = useState(null);

  // The server's price is the one charged, so show it rather than the browser's estimate.
  useEffect(() => {
    if (design) {
      api.quote(design).then(setQuote).catch((e) => setProblem(e.message));
    }
  }, [design]);

  useEffect(() => {
    if (account && account.phone) {
      setPhone((typed) => typed || account.phone);
    }
  }, [account]);

  if (!catalog) {
    return <p className="hint">Loading…</p>;
  }
  if (!design) {
    return <p className="notice">Nothing designed yet. <Link to="/design">Start a design</Link></p>;
  }
  if (!fit) {
    return (
      <>
        <Steps />
        <p className="notice">We need your measurements first. <Link to="/fit">Add measurements</Link></p>
      </>
    );
  }

  const garment = catalog.garments.find((g) => g.code === design.garment);
  const colour = catalog.colours.find((c) => c.code === design.colour) || catalog.colours[0];
  const fabric = catalog.fabrics.find((f) => f.code === design.fabric);
  const choices = garment.parts.map((part) => {
    const option = part.options.find((o) => o.code === design[part.part.toLowerCase()]);
    return [part.label, option ? option.label : '?'];
  });
  const canPlace = account && quote && accepted && phoneIsValid(phone) && address.trim() !== '' && !working;

  async function place(event) {
    event.preventDefault();
    setWorking(true);
    setProblem(null);
    try {
      const order = await api.placeOrder({
        design, measurements: fit.measurements, phone, deliveryAddress: address.trim(),
        notes: notes.trim() || null, acceptsTerms: accepted
      });
      setDesign(null);
      navigate(`/order/${order.orderNumber}`);
    } catch (e) {
      setProblem(e.code === 'VALIDATION_FAILED' ? 'Please check: ' + (e.details || []).join('; ') : e.message);
    } finally {
      setWorking(false);
    }
  }

  return (
    <>
      <Steps />
      <div className="two-col">
        <div className="review-design">
          <GarmentSketch design={design} colourHex={colour.hex} size={200} />
          <div>
            <h2>{garment.name}</h2>
            <dl className="facts">
              {choices.map(([label, value]) => <React.Fragment key={label}><dt>{label}</dt><dd>{value}</dd></React.Fragment>)}
              <dt>Fabric</dt><dd>{fabric ? fabric.name : design.fabric}</dd>
              <dt>Colour</dt><dd><span className="dot" style={{ background: colour.hex }} />{colour.name}</dd>
            </dl>
            <p><Link to="/design">Change the design</Link></p>
            <h3>Measurements</h3>
            <dl className="facts">
              {MEASUREMENTS.map((m) => <React.Fragment key={m.key}><dt>{m.label}</dt><dd>{fit.measurements[m.key]} cm</dd></React.Fragment>)}
            </dl>
            <p><Link to="/fit">Change measurements</Link></p>
          </div>
        </div>

        <div>
          {quote && (
            <dl className="bill">
              <dt>{quote.garmentName}</dt><dd>{money(quote.basePricePaise)}</dd>
              {quote.extras.map((line) => <React.Fragment key={line.label}><dt>{line.label}</dt><dd>{money(line.paise)}</dd></React.Fragment>)}
              <dt>Shipping</dt><dd>{quote.shippingPaise === 0 ? 'Free' : money(quote.shippingPaise)}</dd>
              <dt className="total">Total, tax included</dt><dd className="total">{money(quote.totalPaise)}</dd>
            </dl>
          )}
          {quote && <p className="hint">Made and sent by {calendarDay(quote.readyBy)}. Pay on delivery.</p>}

          {!account ? (
            <p className="notice">
              <Link to="/account" state={{ from: '/review' }}>Sign in or create an account</Link> to order.
              Your design and measurements stay here while you do.
            </p>
          ) : (
            <form onSubmit={place} className="checkout">
              <label>Mobile number for delivery
                <PhoneField value={phone} onChange={setPhone} required />
              </label>
              <label>Delivery address
                <textarea rows={3} value={address} onChange={(e) => setAddress(e.target.value)} maxLength={300} required />
              </label>
              <label>Notes for the tailor (optional)
                <textarea rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={300}
                  placeholder="For example: a little looser at the hips, add pockets" />
              </label>
              <label className="accept">
                <input type="checkbox" checked={accepted} onChange={(e) => setAccepted(e.target.checked)} />
                <span>
                  I understand that plant-dyed shades vary a little from batch to batch, and that a garment made to my
                  measurements can be cancelled only until the fabric is cut. Faults and fit problems are still put right.
                </span>
              </label>
              {problem && <p className="notice error" role="alert">{problem}</p>}
              <button className="primary" disabled={!canPlace}>{working ? 'Placing order…' : `Place order${quote ? ' · ' + money(quote.totalPaise) : ''}`}</button>
            </form>
          )}
        </div>
      </div>
    </>
  );
}
