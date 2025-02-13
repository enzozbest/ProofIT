import React, { FC, useState, useEffect } from 'react';
import NavBar from '../components/NavBar';
import HeroSection from '../components/HeroSection';
import InputBox from '../components/InputBox';
import OldPrompts from '../components/OldPrompts';
import GeneratedPrompts from '../components/GeneratedPrompts';
import BackgroundSVG from '../assets/background.svg';

const LandingPage: FC = () => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isAdmin, setIsAdmin] = useState<boolean>(false);

  useEffect(() => {
    fetch('http://localhost:8000/api/auth/check', {
      method: 'GET',
      credentials: 'include',
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.userId) {
          setIsAuthenticated(true);
          setIsAdmin(data.isAdmin || false);
        } else {
          setIsAuthenticated(false);
          setIsAdmin(false);
        }
      })
      .catch((error) => console.error('Error:', error));
  }, []);

  const prompts: string[] = [
    'Generate code for a chatbot',
    'Build a mobile app for my service',
    'Create a documentation site',
  ];

  return (
    <div
      className="flex flex-col min-h-screen"
      style={{
        backgroundImage: `url(${BackgroundSVG})`,
        backgroundSize: 'cover',
        backgroundRepeat: 'no-repeat',
        backgroundPosition: 'center',
      }}
    >
      <NavBar
        isAuthenticated={isAuthenticated}
        setIsAuthenticated={setIsAuthenticated}
      />
      <div className="flex flex-col items-center justify-center flex-grow w-full px-6">
        <HeroSection />
        {isAuthenticated && (
          <div className="flex justify-center w-full max-w-4xl mt-6">
            <OldPrompts />
          </div>
        )}
        <div className="w-full max-w-5xl mt-6">
          <InputBox />
        </div>
        {!isAuthenticated && (
          <div className="flex justify-center w-full mt-6">
            <GeneratedPrompts prompts={prompts} />
          </div>
        )}
      </div>
    </div>
  );
};

export default LandingPage;
