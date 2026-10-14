import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { FashionProvider } from './fashion';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <BrowserRouter basename={import.meta.env.BASE_URL}>
    <FashionProvider>
      <App />
    </FashionProvider>
  </BrowserRouter>
);
