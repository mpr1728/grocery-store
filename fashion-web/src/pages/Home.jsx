import React from 'react';
import { Link } from 'react-router-dom';
import { TAGLINE } from '../brand';
import { money } from '../format';
import GarmentSketch from '../GarmentSketch';
import { startingDesign, useFashion } from '../fashion';

// The shade each garment card shows, so the home page shows off the palette.
const CARD_COLOURS = { KURTI: 'INDIGO_DEEP', TOP: 'TURMERIC', FROCK: 'MADDER' };

export default function Home() {
  const { catalog, error, setDesign } = useFashion();
  if (error) {
    return <p className="notice error">{error}</p>;
  }
  if (!catalog) {
    return <p className="hint">Loading…</p>;
  }
  return (
    <>
      <section className="intro">
        <h1>{TAGLINE}</h1>
        <p>
          Design a kurti, top, or frock your way. We cut it to your measurements in cotton dyed with plants,
          and make it only after you order.
        </p>
      </section>

      <div className="garments">
        {catalog.garments.map((g) => {
          const design = { ...startingDesign(catalog, g.code), colour: CARD_COLOURS[g.code] || 'INDIGO_DEEP' };
          const colour = catalog.colours.find((c) => c.code === design.colour) || catalog.colours[0];
          return (
            <Link key={g.code} to={`/design/${g.code}`} className="garment-card" onClick={() => setDesign(design)}>
              <GarmentSketch design={design} colourHex={colour.hex} size={140} label={`${g.name} sketch`} />
              <div>
                <h2>{g.name}</h2>
                <p className="desc">{g.description}</p>
                <p className="price">From {money(g.basePricePaise)}</p>
                <p className="unit">Made in about {g.makingDays} days</p>
              </div>
            </Link>
          );
        })}
      </div>

      <h2 className="section-title">How it works</h2>
      <ol className="how">
        <li><b>Design it.</b> Pick the neckline, sleeves, length, and shape. The sketch changes as you go.</li>
        <li><b>Measure once.</b> Five measurements with a tape. We tell you which shapes tend to suit you.</li>
        <li><b>Choose the cloth.</b> Four cottons and ten shades from indigo, madder, turmeric, and other plants.</li>
        <li><b>We make it.</b> Cut and stitched for you, checked, and sent. Follow each stage in your orders.</li>
      </ol>

      <h2 className="section-title">The palette</h2>
      <div className="palette">
        {catalog.colours.map((c) => (
          <div key={c.code} className={c.available ? 'swatch-card' : 'swatch-card out'}>
            <span className="swatch" style={{ background: c.hex }} />
            <b>{c.name}</b>
            <span className="unit">{c.dyeSource}</span>
          </div>
        ))}
      </div>
      <p className="hint">Plant dyes vary a little from batch to batch, so your shade can differ slightly from the screen.</p>
    </>
  );
}
