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
  const buttons = [
    "Generate code for a chatbot",
    "Build a mobile app for my service",
    "Create a documentation site",
  ];

  return (
    <div>
      <LandingPage buttons={buttons} />
      <ColorPreview></ColorPreview>
    </div>
  );
}

export default App;
