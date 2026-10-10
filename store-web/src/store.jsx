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
    // The cart stays saved with the account. This browser starts empty for whoever uses it next.
    setCart({});
    try {
      sessionStorage.removeItem('assistant.chat.v1');
    } catch (e) {
      // Nothing to clear.
    }
  }, []);

  // A signed-in customer's cart is kept with their account, so it is there on their next visit and on
  // their other devices. On sign-in, anything already in this browser's cart is added to the saved one.
  const accountKey = account ? account.email || account.phone : null;
  const [savedCartLoaded, setSavedCartLoaded] = useState(false);

  useEffect(() => {
    setSavedCartLoaded(false);
    if (!accountKey) {
      return undefined;
    }
    let stopped = false;
    api.savedCart()
      .then((saved) => {
        if (stopped) return;
        setCart((local) => {
          const merged = { ...local };
          saved.lines.forEach((line) => {
            merged[line.productId] = Math.max(merged[line.productId] || 0, line.quantity);
          });
          return merged;
        });
        setSavedCartLoaded(true);
      })
      .catch(() => {
        // The cart still works from this browser's copy if the saved one cannot be read.
      });
    return () => { stopped = true; };
  }, [accountKey]);

  useEffect(() => {
    if (!accountKey || !savedCartLoaded) {
      return undefined;
    }
    // Wait for a pause in the taps on plus and minus before saving.
    const timer = setTimeout(() => {
      const lines = Object.entries(cart).map(([productId, quantity]) => ({ productId: Number(productId), quantity }));
      api.saveCart(lines).catch(() => {});
    }, 600);
    return () => clearTimeout(timer);
  }, [cart, accountKey, savedCartLoaded]);

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
