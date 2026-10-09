import React from 'react';
import { MAX_PER_ITEM, useStore } from './store';

export default function Stepper({ item }) {
  const { cart, setQuantity } = useStore();
  const quantity = cart[item.productId] || 0;
  const limit = Math.min(item.available, MAX_PER_ITEM);

  if (item.available <= 0) {
    return <button className="add" disabled>Out of stock</button>;
  }
  if (quantity === 0) {
    return <button className="add" onClick={() => setQuantity(item.productId, 1)}>Add</button>;
  }
  return (
    <div className="stepper">
      <button aria-label={`Remove one ${item.name}`} onClick={() => setQuantity(item.productId, quantity - 1)}>−</button>
      <span aria-live="polite">{quantity}</span>
      <button aria-label={`Add one ${item.name}`} disabled={quantity >= limit} onClick={() => setQuantity(item.productId, quantity + 1)}>+</button>
    </div>
  );
}
