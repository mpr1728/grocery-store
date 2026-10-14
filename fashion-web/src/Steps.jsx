import React from 'react';
import { NavLink } from 'react-router-dom';

/** The three steps of making a garment, shown at the top of each step's page. */
export default function Steps() {
  return (
    <ol className="steps">
      <li><NavLink to="/design">1. Design</NavLink></li>
      <li><NavLink to="/fit">2. Measurements</NavLink></li>
      <li><NavLink to="/review">3. Review and order</NavLink></li>
    </ol>
  );
}
