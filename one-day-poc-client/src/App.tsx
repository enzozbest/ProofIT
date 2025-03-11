import React from 'react';
import { Route, Routes } from 'react-router-dom';
import Generate from './pages/Generate';
import ErrorRoutes from './pages/ErrorPages';

import './App.css';
import LandingPage from './pages/LandingPage';

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/generate" element={<Generate />} />
      <Route path="/*" element={<ErrorRoutes />} />
    </Routes>
  );
};

export default App;
