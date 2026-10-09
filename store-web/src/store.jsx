import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, hasSession, setSessionToken, whenSessionEnds } from './api';

const StoreContext = createContext(null);
const CART_KEY = 'cart.v1';
export const MAX_PER_ITEM = 50;

function loadCart() {
  try {
    const saved = JSON.parse(localStorage.getItem(CART_KEY));
    return saved && typeof saved === 'object' ? saved : {};
  } catch (e) {
    return {};
  }
}

export function StoreProvider({ children }) {
  const [hub, setHub] = useState(null);
  const [info, setInfo] = useState(null);
  const [catalog, setCatalog] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // Cart is a map of product id to quantity.
  const [cart, setCart] = useState(loadCart);
  // The signed-in customer with their points, or null for a guest.
  const [account, setAccount] = useState(null);

  const refreshAccount = useCallback(async () => {
    if (!hasSession()) {
      setAccount(null);
      return;
    }
    try {
      setAccount(await api.me());
    } catch (e) {
      // A session that has ended is cleared by the api layer. Other errors keep what is shown.
    }
  }, []);

  useEffect(() => {
    whenSessionEnds(() => setAccount(null));
    refreshAccount();
  }, [refreshAccount]);

  const signedIn = useCallback((result) => {
    setSessionToken(result.token);
    setAccount(result.account);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await api.signOut();
    } catch (e) {
      // Signing out on this device does not depend on the server answering.
    }
    setSessionToken(null);
    setAccount(null);
  }, []);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [hubs, storeInfo] = await Promise.all([api.hubs(), api.storeInfo()]);
      if (hubs.length === 0) {
        throw new Error('No hub is open right now.');
      }
      setHub(hubs[0]);
      setInfo(storeInfo);
      setCatalog(await api.catalog(hubs[0].id));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    try {
      localStorage.setItem(CART_KEY, JSON.stringify(cart));
    } catch (e) {
      // The cart still works for this visit if storage is unavailable.
    }
  }, [cart]);

  const setQuantity = useCallback((productId, quantity) => {
    setCart((current) => {
      const next = { ...current };
      if (quantity <= 0) {
        delete next[productId];
      } else {
        next[productId] = quantity;
      }
      return next;
    });
  }, []);

  const clearCart = useCallback(() => setCart({}), []);

  const value = useMemo(() => {
    const byId = new Map(catalog.map((item) => [String(item.productId), item]));
    const lines = Object.entries(cart)
      .filter(([productId]) => byId.has(productId))
      .map(([productId, quantity]) => ({ item: byId.get(productId), quantity }));
    const itemTotalPaise = lines.reduce((sum, line) => sum + line.item.pricePaise * line.quantity, 0);
    const itemCount = lines.reduce((sum, line) => sum + line.quantity, 0);
    const deliveryFeePaise = !info || itemTotalPaise === 0 || itemTotalPaise >= info.freeDeliveryAbovePaise ? 0 : info.deliveryFeePaise;
    return {
      hub, info, catalog, loading, error, reload,
      cart, lines, itemCount, itemTotalPaise, deliveryFeePaise,
      totalPaise: itemTotalPaise + deliveryFeePaise,
      setQuantity, clearCart,
      account, refreshAccount, signedIn, signOut
    };
  }, [hub, info, catalog, loading, error, reload, cart, setQuantity, clearCart, account, refreshAccount, signedIn, signOut]);

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useStore() {
  return useContext(StoreContext);
}
