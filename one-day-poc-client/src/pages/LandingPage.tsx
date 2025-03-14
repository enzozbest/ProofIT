import React, { FC, useState, useEffect } from 'react';
import NavBar from '../components/NavBar';
import HeroSection from '../components/HeroSection';
import InputBox from '../components/InputBox';
import OldPrompts from '../components/OldPrompts';
import GeneratedPrompts from '../components/GeneratedPrompts';
import BackgroundSVG from '../assets/background.svg';
import { useAuth } from '../contexts/AuthContext';

const LandingPage: FC = () => {
  const { isAuthenticated } = useAuth();

  const prompts: string[] = [
    'AI chatbot assistant for customer self-service',
    'Dashboard for financial reports',
    'Intelligent document processing tool',
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
      <NavBar />
      <div className="flex flex-col items-center justify-center flex-grow w-full px-6">
        <HeroSection />
        <div className="flex justify-center w-full mt-6">
          <GeneratedPrompts prompts={prompts} />
        </div>
        <div className="w-full max-w-5xl mt-6">
          <InputBox />
        </div>
        {isAuthenticated && (
          <div className="flex justify-center w-full max-w-4xl mt-6">
            <OldPrompts />
          </div>
        )}
      </div>
    </div>
  );
};

export default LandingPage;
