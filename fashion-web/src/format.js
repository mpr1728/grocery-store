const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2, minimumFractionDigits: 0 });

export function money(paise) {
  return rupees.format(paise / 100);
}

export const day = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
export const dateTime = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });

/** A LocalDate from the API ("2026-10-24") read as that calendar day, not midnight UTC. */
export function calendarDay(isoDate) {
  const [y, m, d] = isoDate.split('-').map(Number);
  return day.format(new Date(y, m - 1, d));
}

export const STATUS_LABELS = {
  PLACED: 'Order placed',
  CUTTING: 'Fabric cut',
  STITCHING: 'Being stitched',
  CHECKED: 'Quality checked',
  DISPATCHED: 'On the way',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled'
};

export const STATUS_ORDER = ['PLACED', 'CUTTING', 'STITCHING', 'CHECKED', 'DISPATCHED', 'DELIVERED'];

export const MEASUREMENTS = [
  { key: 'bustCm', label: 'Bust', how: 'Around the fullest part of the chest, tape level under the arms.' },
  { key: 'waistCm', label: 'Waist', how: 'Around the narrowest part, usually just above the navel. Do not pull tight.' },
  { key: 'hipCm', label: 'Hip', how: 'Around the fullest part of the hips, feet together.' },
  { key: 'shoulderCm', label: 'Shoulder', how: 'Across the back, from one shoulder tip to the other.' },
  { key: 'heightCm', label: 'Height', how: 'Standing straight without footwear.' }
];
