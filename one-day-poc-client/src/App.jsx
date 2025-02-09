import { Routes, Route } from "react-router-dom";
import Chat from './pages/Chat'
import Home from './pages/Home'
import Generate1 from "./pages/Generate1";
import { useState } from 'react';
import './App.css';
import LandingPage from "./pages/LandingPage";


function App() {
    return (
        <div>
            <Routes>
                <Route path="/" element={<LandingPage />} />
                <Route path="/chat" element={<Chat />} />
                <Route path="/generate" element={<Generate1 />} />
            </Routes>
        </div>
    );
}

export default App;
