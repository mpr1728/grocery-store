import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, hasSession, setSessionToken, whenSessionEnds } from './api';

const FashionContext = createContext(null);
const DESIGN_KEY = 'fashion.design.v1';

function loadDesign() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(DESIGN_KEY));
    return saved && typeof saved === 'object' ? saved : null;
  } catch (e) {
    return null;
  }
}

/** The design with every part on its preselected choice, the first fabric, and the first shade in stock. */
export function startingDesign(catalog, garmentCode) {
  const garment = catalog.garments.find((g) => g.code === garmentCode) || catalog.garments[0];
  const design = { garment: garment.code };
  garment.parts.forEach((part) => {
    const chosen = part.options.find((o) => o.preselected) || part.options[0];
    design[part.part.toLowerCase()] = chosen.code;
  });
  design.fabric = catalog.fabrics[0].code;
  design.colour = (catalog.colours.find((c) => c.available) || catalog.colours[0]).code;
  return design;
}

/** Works out the price in the browser for instant feedback. The server checks it again when the order is placed. */
export function priceOf(catalog, design) {
  const garment = catalog.garments.find((g) => g.code === design.garment);
  if (!garment) {
    return null;
  }
  const extras = [];
  garment.parts.forEach((part) => {
    const option = part.options.find((o) => o.code === design[part.part.toLowerCase()]);
    if (option && option.surchargePaise > 0) {
      extras.push({ label: `${part.label}: ${option.label}`, paise: option.surchargePaise });
    }
  });
  const fabric = catalog.fabrics.find((f) => f.code === design.fabric);
  if (fabric && fabric.surchargePaise > 0) {
    extras.push({ label: `Fabric: ${fabric.name}`, paise: fabric.surchargePaise });
  }
  const item = garment.basePricePaise + extras.reduce((sum, e) => sum + e.paise, 0);
  const shipping = item >= catalog.freeShippingAbovePaise ? 0 : catalog.shippingPaise;
  return { base: garment.basePricePaise, extras, item, shipping, total: item + shipping };
}

export function FashionProvider({ children }) {
  const [catalog, setCatalog] = useState(null);
  const [error, setError] = useState(null);
  const [account, setAccount] = useState(null);
  // The garment being designed, kept for this tab so a reload or a trip to sign in does not lose it.
  const [design, setDesignState] = useState(loadDesign);
  // Measurements typed this visit. Saved to the account when signed in.
  const [fit, setFit] = useState(null);

  useEffect(() => {
    api.catalog().then(setCatalog).catch((e) => setError(e.message));
  }, []);

  const refreshAccount = useCallback(async () => {
    if (!hasSession()) {
      setAccount(null);
      return;
    }
    try {
      setAccount(await api.me());
    } catch (e) {
      // A session that has ended is cleared by the api layer.
    }
  }, []);

  useEffect(() => {
    whenSessionEnds(() => setAccount(null));
    refreshAccount();
  }, [refreshAccount]);

  // A signed-in customer's saved measurements fill the fit step.
  const signedInAs = account ? account.email || account.phone : null;
  useEffect(() => {
    if (signedInAs) {
      api.savedFit().then((saved) => saved && setFit((typed) => typed || saved)).catch(() => {});
    }
  }, [signedInAs]);

  const setDesign = useCallback((next) => {
    setDesignState(next);
    try {
      sessionStorage.setItem(DESIGN_KEY, JSON.stringify(next));
    } catch (e) {
      // The design still lives for this page.
    }
  }, []);

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
    setFit(null);
  }, []);

  const value = useMemo(() => ({
    catalog, error, account, design, setDesign, fit, setFit, signedIn, signOut, refreshAccount
  }), [catalog, error, account, design, setDesign, fit, signedIn, signOut, refreshAccount]);

  return <FashionContext.Provider value={value}>{children}</FashionContext.Provider>;
}

export function useFashion() {
  return useContext(FashionContext);
}
