import React from 'react';
import { Route, Routes } from 'react-router-dom';
import Generate from './pages/Generate';
import ErrorRoutes from './pages/ErrorPages';

import './App.css';
import LandingPage from './pages/LandingPage';
import ProfilePage from './pages/ProfilePage';
import { AuthProvider } from '@/contexts/AuthContext';
import { ConversationProvider } from '@/contexts/ConversationContext';

/**
 * App Component
 * 
 * Defines the main application structure including:
 * - Global authentication context provider
 * - Primary route definitions
 * - Error handling routes
 * 
 * This component is the root of the React component tree and organizes
 * the application into its main functional sections.
 * 
 * Routes include:
 * - Landing page (/)
 * - User profile (/profile)
 * - Code generation interface (/generate)
 * - Error pages for handling various error conditions (/*) 
 * 
 * @returns {JSX.Element} The complete application structure with routing
 */
const App: React.FC = () => {
  return (
    <AuthProvider>
      <ConversationProvider>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/generate" element={<Generate />} />
          <Route path="/*" element={<ErrorRoutes />} />
        </Routes>
      </ConversationProvider>
    </AuthProvider>
  );
};

export default App;
