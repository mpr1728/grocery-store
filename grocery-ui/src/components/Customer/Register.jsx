import React, { useState } from 'react';
import axios from '../../services/api';
export default function Register() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const handleSubmit = async (e) => {
    e.preventDefault();
    await axios.post('/api/users/register', { email, password });
  };
  return (
    <form onSubmit={handleSubmit}>
      <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
      <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
      <button type="submit">Register</button>
    </form>
  );
}
