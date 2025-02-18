import React from 'react';
import { Routes, Route } from "react-router-dom";
import Generate from "./pages/Generate";

import './App.css';
import LandingPage from './pages/LandingPage';

const App: React.FC = () => {
  return (
    <div>
        <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/generate" element={<Generate />} />
        </Routes>
    </div>
  );
};

export default App;
