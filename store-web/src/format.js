const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2, minimumFractionDigits: 0 });

export function money(paise) {
  return rupees.format(paise / 100);
}

export const STATUS_LABELS = {
  PLACED: 'Order placed',
  PICKING: 'Picking',
  PACKED: 'Packed',
  OUT_FOR_DELIVERY: 'On the way',
  DELIVERED: 'Delivered'
};

export const STATUS_ORDER = ['PLACED', 'PICKING', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED'];

export const POINT_REASONS = {
  WELCOME: 'Welcome gift',
  EARNED: 'Earned on order',
  REDEEMED: 'Used on order'
};
