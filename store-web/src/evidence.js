// Wording for how a product was grown. These are claims, so each one says exactly what backs it.
export const GROWING_LABELS = {
  CERTIFIED_ORGANIC: 'Certified organic',
  GROWER_DECLARED: 'No synthetic pesticides'
};

export const GROWING_DETAIL = {
  CERTIFIED_ORGANIC: 'Certified organic. The certificate is listed below.',
  GROWER_DECLARED: 'The grower declares that no synthetic pesticides were used. This is their statement, not a lab result.'
};

export const TEST_RESULT_LABELS = {
  NONE_DETECTED: 'No pesticide residue detected',
  WITHIN_LIMITS: 'Residue found, within the legal limit'
};

/** Stored image paths are relative to the app's folder. Full addresses are used as they are. */
export function imageSrc(imageUrl) {
  if (!imageUrl) return null;
  return /^https?:/.test(imageUrl) ? imageUrl : import.meta.env.BASE_URL + imageUrl;
}

export function documentHref(documentId) {
  return `${import.meta.env.BASE_URL}api/documents/${documentId}`;
}
