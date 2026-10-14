import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { MEASUREMENTS } from '../format';
import Steps from '../Steps';
import { useFashion } from '../fashion';

const CM_PER_INCH = 2.54;

function shown(cm, unit) {
  if (cm === undefined || cm === null) {
    return '';
  }
  return unit === 'in' ? String(Math.round((cm / CM_PER_INCH) * 2) / 2) : String(cm);
}

export default function Fit() {
  const { catalog, design, setDesign, fit, setFit, account } = useFashion();
  const navigate = useNavigate();
  // Many tailors in India measure in inches, so both are accepted. The API always gets whole centimetres.
  const [unit, setUnit] = useState('cm');
  const [typed, setTyped] = useState(() => {
    const saved = fit ? fit.measurements : {};
    return Object.fromEntries(MEASUREMENTS.map((m) => [m.key, shown(saved[m.key], 'cm')]));
  });
  const [working, setWorking] = useState(false);
  const [problem, setProblem] = useState(null);

  // Saved measurements can arrive after the page opens. Fill them in unless something was typed already.
  useEffect(() => {
    if (fit && Object.values(typed).every((v) => v === '')) {
      setTyped(Object.fromEntries(MEASUREMENTS.map((m) => [m.key, shown(fit.measurements[m.key], unit)])));
    }
    // Only when a reading arrives, not on every keystroke.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fit]);

  function changeUnit(next) {
    if (next === unit) {
      return;
    }
    // Convert what is already typed so nobody has to measure again.
    setTyped(Object.fromEntries(Object.entries(typed).map(([key, value]) => {
      const number = parseFloat(value);
      if (Number.isNaN(number)) {
        return [key, value];
      }
      const cm = unit === 'in' ? number * CM_PER_INCH : number;
      return [key, next === 'in' ? shown(cm, 'in') : String(Math.round(cm))];
    })));
    setUnit(next);
  }

  const inCm = Object.fromEntries(MEASUREMENTS.map((m) => {
    const number = parseFloat(typed[m.key]);
    return [m.key, Number.isNaN(number) ? null : Math.round(unit === 'in' ? number * CM_PER_INCH : number)];
  }));
  const complete = MEASUREMENTS.every((m) => inCm[m.key] !== null);

  async function check(event) {
    event.preventDefault();
    setWorking(true);
    setProblem(null);
    try {
      // Signed-in customers keep their measurements for next time. Guests only get the reading.
      setFit(account ? await api.saveFit(inCm) : await api.fit(inCm));
    } catch (e) {
      setProblem(e.code === 'VALIDATION_FAILED'
        ? 'Some numbers look off. Check that the unit is right: ' + (e.details || []).join('; ')
        : e.message);
    } finally {
      setWorking(false);
    }
  }

  const garment = catalog && design ? catalog.garments.find((g) => g.code === design.garment) : null;
  const fitPart = garment ? garment.parts.find((p) => p.part === 'FIT') : null;
  const suitedHere = fit && fitPart ? fitPart.options.filter((o) => fit.suitedFits.includes(o.code)) : [];
  const currentFitSuits = suitedHere.some((o) => o.code === (design && design.fit));
  // Shows a result only while it matches what is in the boxes.
  const current = fit && MEASUREMENTS.every((m) => fit.measurements[m.key] === inCm[m.key]);

  return (
    <>
      <Steps />
      <div className="two-col">
        <form onSubmit={check} className="checkout measure">
          <div className="measure-head">
            <h2>Your measurements</h2>
            <div className="unit-toggle" role="radiogroup" aria-label="Unit">
              {['cm', 'in'].map((u) => (
                <button type="button" key={u} role="radio" aria-checked={unit === u} className={unit === u ? 'chip on' : 'chip'} onClick={() => changeUnit(u)}>
                  {u === 'cm' ? 'Centimetres' : 'Inches'}
                </button>
              ))}
            </div>
          </div>
          <p className="hint">Measure over light clothing with a soft tape, snug but not tight. We add the right ease for the fit you choose.</p>
          {MEASUREMENTS.map((m) => (
            <label key={m.key}>
              <span><b>{m.label}</b> · {m.how}</span>
              <span className="with-unit">
                <input inputMode="decimal" value={typed[m.key]} required
                  onChange={(e) => setTyped({ ...typed, [m.key]: e.target.value.replace(/[^0-9.]/g, '').slice(0, 5) })} />
                <span>{unit}</span>
              </span>
            </label>
          ))}
          {problem && <p className="notice error" role="alert">{problem}</p>}
          <button className="primary" disabled={working || !complete}>{working ? 'One moment…' : 'Check my shape'}</button>
          {!account && <p className="hint"><Link to="/account" state={{ from: '/fit' }}>Sign in</Link> to save these for your next order.</p>}
        </form>

        <div>
          {current ? (
            <div className="shape-card">
              <p className="unit">Your shape reads as</p>
              <h2>{fit.shapeLabel}</h2>
              <p>{fit.advice}</p>
              {garment && suitedHere.length > 0 && (
                <p>
                  For a {garment.name.toLowerCase()}, try: <b>{suitedHere.map((o) => o.label).join(', ')}</b>.
                  {!currentFitSuits && (
                    <> <button className="link" onClick={() => setDesign({ ...design, fit: suitedHere[0].code })}>Switch to {suitedHere[0].label}</button></>
                  )}
                </p>
              )}
              <p className="hint">This is a styling guide only. Your garment is cut to your numbers whichever shape you choose.</p>
              <button className="primary wide" onClick={() => navigate('/review')} disabled={!design}>Next: review</button>
            </div>
          ) : (
            <div className="shape-card quiet">
              <h2>Which shapes suit you?</h2>
              <p className="hint">Enter all five measurements and press Check. We read your shape from bust, waist, and hip and point out the fits that tend to flatter it.</p>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
