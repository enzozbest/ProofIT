import React, { FC } from 'react';
import NavBar from '../components/NavBar';
import HeroSection from '../components/landing/HeroSection';
import InputBox from '../components/landing/InputBox';
import OldPrompts from '../components/landing/OldPrompts';
import GeneratedPrompts from '../components/landing/GeneratedPrompts';
import BackgroundSVG from '../assets/background.svg';
import { useAuth } from '../contexts/AuthContext';

/**
 * Landing Page Component
 *
 * Renders the application's main landing page with:
 * - Navigation bar for authentication and branding
 * - Hero section with main value proposition
 * - Suggested prompts to help users get started
 * - Input box for users to enter their own prompts
 * - Previous prompt history for authenticated users
 *
 * The page features a gradient background and responsive layout that
 * centers content on different screen sizes.
 *
 * @component
 * @returns {JSX.Element} The complete landing page with conditional sections based on auth state
 */
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
      <div className="flex flex-col items-center justify-center flex-grow w-full px-6 pt-20">
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
