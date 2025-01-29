import React from "react";
import NavBar from "../components/NavBar";
import HeroSection from "../components/HeroSection";
import InputBox from "../components/InputBox";
import OldPrompts from "../components/OldPrompts";

const LandingPage = ({ buttons }) => {
  return (
    <div className="flex flex-col min-h-screen">
      <NavBar />
      <div className="flex flex-col items-center justify-center flex-grow">
        <HeroSection />
        <OldPrompts />
        <InputBox />
        <div className="mt-5 flex gap-3">
          {buttons.map((text, index) => (
            <button key={index} className="border px-4 py-2 rounded-lg hover:opacity-80">
              {text} →
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LandingPage;
