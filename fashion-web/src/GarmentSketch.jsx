import React, { useId } from 'react';

// A flat front sketch of the design, drawn from the chosen codes. It shows proportions, not an exact pattern.
// Coordinates are in a 200 by 300 box: shoulders near y 44, waist at y 120, the centre line at x 100.

const WAIST_Y = 120;

const HEM_Y = {
  TOP: { CROP: 132, WAIST: 150, HIP: 172 },
  KURTI: { HIP: 176, KNEE: 234, CALF: 262, ANKLE: 288 },
  FROCK: { KNEE: 224, MIDI: 254, MAXI: 288 }
};

// Each neckline: where it meets the shoulders (left x, y) and the path across to the right side.
const NECKS = {
  ROUND: { x: 86, y: 38, path: 'Q100,60 114,38' },
  V: { x: 86, y: 38, path: 'L100,74 L114,38' },
  BOAT: { x: 76, y: 41, path: 'Q100,50 124,41' },
  SQUARE: { x: 86, y: 38, path: 'L86,58 L114,58 L114,38' },
  SWEETHEART: { x: 86, y: 40, path: 'L86,56 Q93,66 100,58 Q107,66 114,56 L114,40' },
  MANDARIN: { x: 88, y: 37, path: 'Q100,46 112,37' },
  KEYHOLE: { x: 86, y: 38, path: 'Q100,52 114,38' }
};

// Right sleeve only. The left one is the mirror image.
const SLEEVES = {
  SLEEVELESS: null,
  CAP: 'M136,44 L153,60 L142,73 L134,70 Z',
  PUFF: 'M136,44 Q170,50 150,80 Q142,82 134,76 Z',
  ELBOW: 'M136,44 L163,110 L147,117 L134,84 Z',
  THREE_QUARTER: 'M136,44 L169,140 L153,146 L134,84 Z',
  FULL: 'M136,44 L175,182 L159,186 L134,84 Z'
};

/** Half widths at the waist and the hem for a fit, given how far the hem is below the waist. */
function widths(fit, drop) {
  switch (fit) {
    case 'FITTED': return { waist: 29, hem: 36 };
    case 'RELAXED': return { waist: 37, hem: 40 };
    case 'A_LINE': return { waist: 33, hem: 36 + drop * 0.3 };
    case 'FLARED': return { waist: 33, hem: 38 + drop * 0.55 };
    case 'FIT_AND_FLARE': return { waist: 29, hem: 38 + drop * 0.55 };
    case 'TIERED': return { waist: 30, hem: 38 + drop * 0.45 };
    case 'PEPLUM': return { waist: 29, hem: 46 };
    default: return { waist: 34, hem: 37 };
  }
}

function darker(hex, amount) {
  const n = parseInt(hex.slice(1), 16);
  const part = (shift) => Math.max(0, Math.round(((n >> shift) & 255) * (1 - amount)));
  return `rgb(${part(16)},${part(8)},${part(0)})`;
}

export default function GarmentSketch({ design, colourHex, size = 260, label }) {
  const patternId = useId().replace(/:/g, '');
  const { garment, neckline, sleeve, length, fit, fabric } = design;
  const hemY = (HEM_Y[garment] || HEM_Y.KURTI)[length] || 234;
  const w = widths(fit, hemY - WAIST_Y);
  const neck = NECKS[neckline] || NECKS.ROUND;
  const right = (x) => 100 + x;
  const left = (x) => 100 - x;

  const body = [
    `M${neck.x},${neck.y}`,
    neck.path,
    'L136,44 L134,84',
    `L${right(w.waist)},${WAIST_Y}`,
    `L${right(w.hem)},${hemY}`,
    `Q100,${hemY + 6} ${left(w.hem)},${hemY}`,
    `L${left(w.waist)},${WAIST_Y}`,
    'L66,84 L64,44 Z'
  ].join(' ');

  const stroke = darker(colourHex, 0.35);
  const fill = fabric === 'KHADI' ? `url(#${patternId})` : colourHex;
  const seams = [];
  if (fit === 'FIT_AND_FLARE' || fit === 'PEPLUM' || garment === 'FROCK') {
    seams.push(`M${left(w.waist)},${WAIST_Y} L${right(w.waist)},${WAIST_Y}`);
  }
  if (fit === 'TIERED') {
    [1, 2].forEach((tier) => {
      const y = WAIST_Y + ((hemY - WAIST_Y) * tier) / 3;
      const half = w.waist + ((w.hem - w.waist) * tier) / 3;
      seams.push(`M${left(half)},${y} Q100,${y + 4} ${right(half)},${y}`);
    });
  }
  if (garment === 'KURTI' && hemY > 160) {
    // Side slits, the usual kurti finish.
    seams.push(`M${right(w.waist + 3)},${WAIST_Y + 30} L${right(w.hem - 2)},${hemY - 2}`);
    seams.push(`M${left(w.waist + 3)},${WAIST_Y + 30} L${left(w.hem - 2)},${hemY - 2}`);
  }

  return (
    <svg viewBox="0 0 200 300" width={size} height={size * 1.5} role="img" aria-label={label || 'Sketch of your design'} className="sketch">
      <defs>
        <pattern id={patternId} width="14" height="10" patternUnits="userSpaceOnUse">
          <rect width="14" height="10" fill={colourHex} />
          <path d="M1,3 h5 M8,8 h4 M3,7 h2" stroke={stroke} strokeOpacity="0.25" strokeWidth="1" />
        </pattern>
      </defs>
      {/* Dress form, so the garment reads as worn. */}
      <rect x="93" y="14" width="14" height="28" rx="5" fill="var(--form)" />
      <line x1="100" y1={hemY} x2="100" y2="298" stroke="var(--form)" strokeWidth="4" />
      <g fill={fill} stroke={stroke} strokeWidth="1.6" strokeLinejoin="round" opacity={fabric === 'MULMUL' ? 0.92 : 1}>
        {SLEEVES[sleeve] && (
          <>
            <path d={SLEEVES[sleeve]} />
            <path d={SLEEVES[sleeve]} transform="translate(200,0) scale(-1,1)" />
          </>
        )}
        <path d={body} />
      </g>
      <g fill="none" stroke={stroke} strokeWidth="1.2" strokeOpacity="0.7">
        {seams.map((d) => <path key={d} d={d} />)}
        {neckline === 'MANDARIN' && <path d="M88,37 Q100,46 112,37 L112,31 Q100,39 88,31 Z" fill={fill} strokeOpacity="1" />}
        {neckline === 'KEYHOLE' && <path d="M100,48 Q95,58 100,66 Q105,58 100,48 Z" fill="var(--surface)" />}
      </g>
    </svg>
  );
}
