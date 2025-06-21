import React, { useState } from 'react';
import axios from '../../services/api';

export default function AddProductForm() {
  const [product, setProduct] = useState({ name: '', price: '', category: '' });
  const handleChange = (e) => {
    setProduct({ ...product, [e.target.name]: e.target.value });
  };
  const handleSubmit = async (e) => {
    e.preventDefault();
    await axios.post('/api/products', product);
  };
  return (
    <form onSubmit={handleSubmit}>
      <input name="name" value={product.name} onChange={handleChange} placeholder="Name" required />
      <input name="price" value={product.price} onChange={handleChange} placeholder="Price" required />
      <input name="category" value={product.category} onChange={handleChange} placeholder="Category" required />
      <button type="submit">Add Product</button>
    </form>
  );
}
