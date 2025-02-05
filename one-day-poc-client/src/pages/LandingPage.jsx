import React, { useState, useEffect } from "react";
import NavBar from "../components/NavBar";
import HeroSection from "../components/HeroSection";
import InputBox from "../components/InputBox";
import OldPrompts from "../components/OldPrompts";
import GeneratedPrompts from "../components/GeneratedPrompts";

const LandingPage = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);  

  useEffect(() => {
    //check if the user is authenticated
    fetch("http://localhost:8000/api/auth/check", {
      method: "GET",
      credentials: "include",
    })
      .then((response )=> response.json()) 
      .then((data) => {
        if(data.userId) {
          setIsAuthenticated(true);
          setIsAdmin(data.isAdmin || false);
        } else {
          setIsAuthenticated(false);
          setIsAdmin(false);
        }
      })
      .catch((error) => console.error("Error:", error));
  }, []);

  const prompts = [
    "Generate code for a chatbot",
    "Build a mobile app for my service",
    "Create a documentation site",
  ];
  return (
    <div className="flex flex-col min-h-screen">
      <NavBar 
        isAuthenticated={isAuthenticated} 
        setIsAuthenticated={setIsAuthenticated} 
        isAdmin={isAdmin}
        setIsAdmin={setIsAdmin}
      />
      <div className="flex flex-col items-center justify-center flex-grow pb-9">
        <HeroSection />
        {isAuthenticated && <OldPrompts />}
        <InputBox />
        <GeneratedPrompts prompts={prompts}/>
      </div>
    </div>
  );
};

export default LandingPage;
