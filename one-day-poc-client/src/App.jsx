import { Routes, Route } from "react-router-dom";
import Home from './pages/Home'
import Generate from "./pages/Generate";
import { useState } from 'react';
import './App.css';
import LandingPage from "./pages/LandingPage";


function App() {
    return (
        <div>
            <Routes>
                <Route path="/" element={<LandingPage />} />
                <Route path="/generate" element={<Generate />} />
            </Routes>
        </div>
    );
}

export default App;
