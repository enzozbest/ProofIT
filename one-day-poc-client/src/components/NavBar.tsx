import React, { FC } from 'react';
import { BotMessageSquare } from 'lucide-react';

type NavBarProps = {
  isAuthenticated: boolean;
  setIsAuthenticated: React.Dispatch<React.SetStateAction<boolean>>;
};

const NavBar: React.FC<NavBarProps> = ({
  isAuthenticated,
  setIsAuthenticated,
}) => {
  const handleSignIn = () => {
    window.location.href = 'http://localhost:8000/api/auth';
  };

  const handleSignOut = () => {
    fetch('http://localhost:8000/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })
      .then(() => {
        setIsAuthenticated(false);
      })
      .catch((error) => console.error('Error:', error));
  };

  return (
    <nav className="flex justify-between items-center w-full px-10 py-4 shadow-md border-b bg-white">
      <div className="flex items-center gap-3 text-xl font-bold">
        <BotMessageSquare size={32} className="text-gray-700" />
        <span className="text-gray-900">ProofIt!</span>
      </div>

      <div>
        {isAuthenticated ? (
          <button
            onClick={handleSignOut}
            className="px-4 py-2 rounded-lg bg-[#213547] text-white hover:opacity-80 transition"
          >
            Log Out
          </button>
        ) : (
          <button
            onClick={handleSignIn}
            className="px-4 py-2 rounded-lg bg-[#213547] text-white hover:opacity-80 transition"
          >
            Sign In
          </button>
        )}
      </div>
    </nav>
  );
};

export default NavBar;
