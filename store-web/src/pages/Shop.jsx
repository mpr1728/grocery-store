import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import Stepper from '../Stepper';
import { GROWING_LABELS, imageSrc } from '../evidence';
import { money } from '../format';
import { useStore } from '../store';
import { TAGLINE } from '../brand';

const PAGE = 48;

export default function Shop() {
  const { catalog, loading, error, reload, itemCount, itemTotalPaise } = useStore();
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All');
  // A long list is shown a page at a time so the shop stays quick.
  const [shown, setShown] = useState(PAGE);

  const categories = useMemo(() => ['All', ...new Set(catalog.map((item) => item.category))], [catalog]);

  const visible = useMemo(() => {
    const text = query.trim().toLowerCase();
    return catalog.filter((item) =>
      (category === 'All' || item.category === category) &&
      (text === '' || item.name.toLowerCase().includes(text) || (item.brand || '').toLowerCase().includes(text)));
  }, [catalog, query, category]);

  if (loading) {
    return <p className="notice">Loading the shop…</p>;
  }
  if (error) {
    return <p className="notice error">{error} <button className="link" onClick={reload}>Try again</button></p>;
  }

  return (
    <>
      <div className="hero">
        <img src={import.meta.env.BASE_URL + 'logo.png'} alt="" />
        <p>{TAGLINE}</p>
      </div>
      <input
        className="search"
        type="search"
        placeholder="Search for milk, atta, batter…"
        aria-label="Search products"
        value={query}
        onChange={(e) => { setQuery(e.target.value); setShown(PAGE); }}
      />
      <div className="chips" role="tablist" aria-label="Categories">
        {categories.map((name) => (
          <button key={name} role="tab" aria-selected={name === category} className={name === category ? 'chip on' : 'chip'} onClick={() => { setCategory(name); setShown(PAGE); }}>
            {name}
          </button>
        ))}
      </div>

      {visible.length === 0 && <p className="notice">Nothing matches "{query}". Try a shorter word.</p>}

      <div className="grid">
        {visible.slice(0, shown).map((item) => (
          <article key={item.productId} className={item.available <= 0 ? 'card out' : 'card'}>
            <Link to={`/product/${item.productId}`} className={item.imageUrl ? 'tile photo' : 'tile'} data-category={item.category} aria-label={`${item.name}, details`}>
              {item.imageUrl && <img src={imageSrc(item.imageUrl)} alt="" loading="lazy" />}
              <span className={item.diet === 'VEG' ? 'diet veg' : 'diet nonveg'} title={item.diet === 'VEG' ? 'Vegetarian' : 'Non-vegetarian'} />
              {item.privateLabel && <span className="tag">House brand</span>}
            </Link>
            <h3><Link to={`/product/${item.productId}`}>{item.name}</Link></h3>
            <p className="unit">{[item.brand, item.unitLabel].filter(Boolean).join(' · ')}</p>
            {(item.growingMethod || item.testedBatches > 0) && (
              <p className="claims">
                {item.growingMethod && <span className="badge">{GROWING_LABELS[item.growingMethod]}</span>}
                {item.testedBatches > 0 && <span className="badge">Residue tested</span>}
              </p>
            )}
            {item.description && <p className="desc">{item.description}</p>}
            <div className="buy">
              <span className="price">{money(item.pricePaise)}</span>
              <Stepper item={item} />
            </div>
            {item.available > 0 && item.available <= 5 && <p className="low">Only {item.available} left</p>}
          </article>
        ))}
      </div>

      {visible.length > shown && (
        <p className="more">
          <button className="small" onClick={() => setShown(shown + PAGE)}>Show more</button>
          <span className="unit">Showing {shown} of {visible.length}</span>
        </p>
      )}

      {itemCount > 0 && (
        <Link to="/cart" className="cartbar">
          <span>{itemCount} {itemCount === 1 ? 'item' : 'items'} · {money(itemTotalPaise)}</span>
          <span>View cart →</span>
        </Link>
      )}
    </>
  );
}
