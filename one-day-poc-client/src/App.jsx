import { Routes, Route } from "react-router-dom";
import Chat from './pages/Chat'
import Home from './pages/Home'
import Generate1 from "./pages/Generate1";
import { useState } from 'react';
import './App.css';
import LandingPage from "./pages/LandingPage";

function ColorPreview() {
    return (
        <div className="grid grid-cols-4 gap-4 p-4">
            <div className="h-16 w-16 bg-primary rounded"></div>
            <div className="h-16 w-16 bg-secondary rounded"></div>
            <div className="h-16 w-16 bg-accent rounded"></div>
            <div className="h-16 w-16 bg-destructive rounded"></div>
        </div>
    );
}

function App() {
    return (
        <div>
            <LandingPage />
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/chat" element={<Chat />} />
                <Route path="/generate" element={<Generate1 />} />
            </Routes>
            <ColorPreview /> {/* If you want to include the color preview */}
        </div>
    );
}

export default App;
