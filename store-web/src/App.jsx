import React from 'react';
import { Link, NavLink, Route, Routes } from 'react-router-dom';
import { BRAND, BRAND_SECTION } from './brand';
import { useStore } from './store';
import Shop from './pages/Shop';
import Cart from './pages/Cart';
import OrderPage from './pages/OrderPage';
import Console from './pages/Console';
import Account from './pages/Account';
import Assistant from './pages/Assistant';
import ProductPage from './pages/ProductPage';

function firstName(name) {
  return (name || 'Account').trim().split(/\s+/)[0];
}

export default function App() {
  const { hub, info, itemCount, account } = useStore();
  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <Link to="/" className="brand">
            <img src={import.meta.env.BASE_URL + 'logo.png'} alt="" width="36" height="36" />
            {BRAND} <span>{BRAND_SECTION}</span>
          </Link>
          {hub && <div className="hub-note">Delivering from {hub.name}</div>}
          <nav>
            <NavLink to="/" end>Shop</NavLink>
            {info && info.assistantEnabled && <NavLink to="/assistant">Ask AI</NavLink>}
            <NavLink to="/cart">Cart{itemCount > 0 && <b className="count">{itemCount}</b>}</NavLink>
            <NavLink to="/account">
              {account ? <>{firstName(account.name)}<b className="count points">{account.pointsBalance} pts</b></> : 'Sign in'}
            </NavLink>
          </nav>
        </div>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Shop />} />
          <Route path="/product/:productId" element={<ProductPage />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/order/:orderNumber" element={<OrderPage />} />
          <Route path="/console" element={<Console />} />
          <Route path="/account" element={<Account />} />
          <Route path="/assistant" element={<Assistant />} />
          <Route path="*" element={<p className="notice">That page does not exist. <Link to="/">Back to the shop</Link></p>} />
        </Routes>
      </main>
    </div>
  );
}
