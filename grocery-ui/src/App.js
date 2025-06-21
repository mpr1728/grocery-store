import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Register from './components/Customer/Register';
import Login from './components/Customer/Login';
import ProductList from './components/Customer/ProductList';
import MembershipPlans from './components/Customer/MembershipPlans';
import AdminDashboard from './components/Admin/AdminDashboard';
import AddProductForm from './components/Admin/AddProductForm';
import Navbar from './components/Shared/Navbar';

function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route path="/products" element={<ProductList />} />
        <Route path="/membership" element={<MembershipPlans />} />
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/admin/add-product" element={<AddProductForm />} />
      </Routes>
    </BrowserRouter>
  );
}
export default App;
