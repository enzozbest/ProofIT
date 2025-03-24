import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import Logo from '@/components/Logo';

/**
 * NavBar component provides the application's top navigation bar with authentication controls.
 *
 * Displays the application logo/name and provides authentication functionality with
 * sign-in and sign-out buttons that adapt based on the user's authentication status.
 * The component makes API calls to handle authentication actions.
 *
 * @component
 * @returns {JSX.Element} A navigation bar with application branding and authentication buttons
 */
const NavBar: React.FC = () => {
  const { isAuthenticated, login, logout } = useAuth();

  return (
    <nav className="fixed top-2 left-0 w-full h-16 flex justify-between items-center px-10 py-6 z-50">
      <Logo />
      <div className="flex gap-2">
        {isAuthenticated ? (
          <button
            onClick={logout}
            className="border-2 border-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition"
          >
            Log Out
          </button>
        ) : (
          <button
            onClick={login}
            className="border-2 border-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition"
          >
            Sign In
          </button>
        )}
      </div>
    </nav>
  );
};

export default NavBar;
