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
    </div>
  );
}

export default App;
