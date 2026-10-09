import React from 'react';

// Countries the store takes mobile numbers from. Both use 10 digit national numbers.
const COUNTRIES = [
  { code: '+91', label: 'India +91', pattern: /^[6-9][0-9]{9}$/ },
  { code: '+1', label: 'USA +1', pattern: /^[2-9][0-9]{9}$/ }
];

/** Splits a stored number such as "+919876543210" into its country and the 10 digits. */
function parts(value) {
  const country = COUNTRIES.find((c) => value.startsWith(c.code)) || COUNTRIES[0];
  return { country, digits: value.startsWith('+') ? value.slice(country.code.length) : value };
}

export function phoneIsValid(value) {
  const { country, digits } = parts(value || '');
  return country.pattern.test(digits);
}

/** value is the full number with country code, or '' while nothing is typed. */
export default function PhoneField({ value, onChange, required }) {
  const { country, digits } = parts(value || '');
  const change = (code, typed) => {
    const clean = typed.replace(/\D/g, '').slice(0, 10);
    // Keep the chosen country while the box is empty by storing just the code.
    onChange(clean === '' && code === COUNTRIES[0].code ? '' : code + clean);
  };
  return (
    <span className="phone-field">
      <select value={country.code} onChange={(e) => change(e.target.value, digits)} aria-label="Country code">
        {COUNTRIES.map((c) => <option key={c.code} value={c.code}>{c.label}</option>)}
      </select>
      <input value={digits} onChange={(e) => change(country.code, e.target.value)} inputMode="numeric" autoComplete="tel-national" placeholder="10 digit number" required={required} />
    </span>
  );
}

/** True when nothing but, at most, a country code has been entered. */
export function phoneIsEmpty(value) {
  return parts(value || '').digits === '';
}
