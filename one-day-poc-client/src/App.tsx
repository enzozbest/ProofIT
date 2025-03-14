import React from 'react';
import { Route, Routes } from 'react-router-dom';
import Generate from './pages/Generate';
import ErrorRoutes from './pages/ErrorPages';

import './App.css';
import LandingPage from './pages/LandingPage';
import ProfilePage from './pages/ProfilePage';

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/generate" element={<Generate />} />
      <Route path="/*" element={<ErrorRoutes />} />
    </Routes>
  );
};

export default App;
