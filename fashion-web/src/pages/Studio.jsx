import React, { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { money } from '../format';
import GarmentSketch from '../GarmentSketch';
import Steps from '../Steps';
import { priceOf, startingDesign, useFashion } from '../fashion';

export default function Studio() {
  const { catalog, error, design, setDesign, fit } = useFashion();
  const { garment: garmentParam } = useParams();
  const navigate = useNavigate();

  // Start a fresh design when there is none yet, or when the address names a different garment.
  useEffect(() => {
    if (!catalog) {
      return;
    }
    const known = garmentParam && catalog.garments.some((g) => g.code === garmentParam);
    if (!design || (known && design.garment !== garmentParam)) {
      setDesign(startingDesign(catalog, known ? garmentParam : design ? design.garment : undefined));
    }
  }, [catalog, design, garmentParam, setDesign]);

  if (error) {
    return <p className="notice error">{error}</p>;
  }
  if (!catalog || !design) {
    return <p className="hint">Loading…</p>;
  }

  const garment = catalog.garments.find((g) => g.code === design.garment) || catalog.garments[0];
  const colour = catalog.colours.find((c) => c.code === design.colour) || catalog.colours[0];
  const price = priceOf(catalog, design);
  const suited = fit ? new Set(fit.suitedFits) : null;

  function switchGarment(code) {
    // Keep the cloth and shade, reset the parts, which differ per garment.
    setDesign({ ...startingDesign(catalog, code), fabric: design.fabric, colour: design.colour });
    navigate(`/design/${code}`, { replace: true });
  }

  const choose = (field, code) => setDesign({ ...design, [field]: code });

  return (
    <>
      <Steps />
      <div className="studio">
        <div className="preview">
          <GarmentSketch design={design} colourHex={colour.hex} />
          <p className="unit">{colour.name} · {(catalog.fabrics.find((f) => f.code === design.fabric) || {}).name}</p>
        </div>

        <div className="controls">
          <div className="chips" role="radiogroup" aria-label="Garment">
            {catalog.garments.map((g) => (
              <button key={g.code} role="radio" aria-checked={g.code === garment.code}
                className={g.code === garment.code ? 'chip on' : 'chip'} onClick={() => switchGarment(g.code)}>
                {g.name}
              </button>
            ))}
          </div>

          {garment.parts.map((part) => {
            const field = part.part.toLowerCase();
            return (
              <fieldset key={part.part} className="part">
                <legend>{part.label}</legend>
                <div className="options">
                  {part.options.map((o) => (
                    <button key={o.code} aria-pressed={design[field] === o.code}
                      className={design[field] === o.code ? 'option on' : 'option'} onClick={() => choose(field, o.code)}>
                      {o.label}
                      {o.surchargePaise > 0 && <span className="extra">+{money(o.surchargePaise)}</span>}
                      {field === 'fit' && suited && suited.has(o.code) && <span className="suits">Suits your shape</span>}
                    </button>
                  ))}
                </div>
              </fieldset>
            );
          })}

          <fieldset className="part">
            <legend>Fabric</legend>
            <div className="fabrics">
              {catalog.fabrics.map((f) => (
                <label key={f.code} className={design.fabric === f.code ? 'fabric on' : 'fabric'}>
                  <input type="radio" name="fabric" checked={design.fabric === f.code} onChange={() => choose('fabric', f.code)} />
                  <span>
                    <b>{f.name}</b>{f.surchargePaise > 0 && <span className="extra">+{money(f.surchargePaise)}</span>}
                    <span className="unit">{f.description}</span>
                  </span>
                </label>
              ))}
            </div>
          </fieldset>

          <fieldset className="part">
            <legend>Colour <span className="unit inline">{colour.name}, from {colour.dyeSource.toLowerCase()}</span></legend>
            <div className="swatches">
              {catalog.colours.map((c) => (
                <button key={c.code} className={design.colour === c.code ? 'swatch-pick on' : 'swatch-pick'}
                  style={{ background: c.hex }} title={c.available ? c.name : `${c.name} (out of stock)`}
                  aria-label={c.name} aria-pressed={design.colour === c.code} disabled={!c.available}
                  onClick={() => choose('colour', c.code)} />
              ))}
            </div>
          </fieldset>

          <div className="summary">
            <div>
              <span className="price big">{money(price.item)}</span>
              <span className="unit">
                {price.shipping === 0 ? 'Free shipping' : `+ ${money(price.shipping)} shipping, free from ${money(catalog.freeShippingAbovePaise)}`}
                {' · '}made in about {garment.makingDays} days
              </span>
            </div>
            <button className="primary" onClick={() => navigate('/fit')}>Next: measurements</button>
          </div>
        </div>
      </div>
    </>
  );
}
