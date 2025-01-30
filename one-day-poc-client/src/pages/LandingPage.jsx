import React from "react";
import NavBar from "../components/NavBar";
import HeroSection from "../components/HeroSection";
import InputBox from "../components/InputBox";
import OldPrompts from "../components/OldPrompts";
import GeneratedPrompts from "../components/GeneratedPrompts";

const LandingPage = () => {
  const prompts = [
    "Generate code for a chatbot",
    "Build a mobile app for my service",
    "Create a documentation site",
  ];
  return (
    <div className="flex flex-col min-h-screen">
      <NavBar />
      <div className="flex flex-col items-center justify-center flex-grow pb-9">
        <HeroSection />
        <OldPrompts />
        <InputBox />
        <GeneratedPrompts prompts={prompts}/>
      </div>
    </div>
  );
};

export default LandingPage;
