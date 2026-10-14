// Panyaloka is the parent brand. Each section has its own front end and, once a domain is bought, its own
// subdomain (fashion.panyaloka.com). Change the names here and in index.html if the brand changes.
export const BRAND = 'Panyaloka';
export const BRAND_SECTION = 'Fashion';
export const TAGLINE = 'Cut to you. Coloured by plants.';

// Where the grocery section lives. On the dev site both sections share one address, so a path is enough.
export const FRESH_URL = import.meta.env.VITE_FRESH_URL || '/grocery/';
