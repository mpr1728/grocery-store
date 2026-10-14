import React from 'react';
import { Link, NavLink, Route, Routes } from 'react-router-dom';
import { BRAND, BRAND_SECTION, FRESH_URL } from './brand';
import { useFashion } from './fashion';
import Home from './pages/Home';
import Studio from './pages/Studio';
import Fit from './pages/Fit';
import Review from './pages/Review';
import Orders from './pages/Orders';
import OrderPage from './pages/OrderPage';
import Account from './pages/Account';
import StudioConsole from './pages/StudioConsole';

function firstName(name) {
  return (name || 'Account').trim().split(/\s+/)[0];
}

export default function App() {
  const { account } = useFashion();
  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <Link to="/" className="brand">
            <img src={import.meta.env.BASE_URL + 'logo.svg'} alt="" width="34" height="34" />
            {BRAND} <span>{BRAND_SECTION}</span>
          </Link>
          <nav>
            <NavLink to="/design">Design</NavLink>
            {account && <NavLink to="/orders">Orders</NavLink>}
            <NavLink to="/account">{account ? firstName(account.name) : 'Sign in'}</NavLink>
          </nav>
        </div>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/design" element={<Studio />} />
          <Route path="/design/:garment" element={<Studio />} />
          <Route path="/fit" element={<Fit />} />
          <Route path="/review" element={<Review />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/order/:orderNumber" element={<OrderPage />} />
          <Route path="/account" element={<Account />} />
          <Route path="/studio" element={<StudioConsole />} />
          <Route path="*" element={<p className="notice">That page does not exist. <Link to="/">Back to the start</Link></p>} />
        </Routes>
      </main>
      <footer className="footer">
        <span>Also from {BRAND}:</span> <a href={FRESH_URL}>{BRAND} Fresh, natural groceries</a>
        <span className="hint"> · One account works in both.</span>
      </footer>
    </div>
  );
}
