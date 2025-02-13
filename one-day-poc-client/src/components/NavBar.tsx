import React, { FC } from 'react';
import { BotMessageSquare } from 'lucide-react';

type NavBarProps = {
  isAuthenticated: boolean;
  setIsAuthenticated: (value: boolean) => void;
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
    <nav className="absolute top-0 left-0 w-full flex justify-between items-center px-10 py-6 z-10">
      <div className="flex items-center gap-2 text-xl text-white">
        <BotMessageSquare className="w-6 h-6 text-white" />
        <span className="font-normal">
          PROOF -<span className="font-bold"> IT!</span>
        </span>
      </div>

      <div className="flex gap-2">
        {isAuthenticated ? (
          <button
            onClick={handleSignOut}
            className="border-2 border-white bg-transparent text-white px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition"
          >
            Log Out
          </button>
        ) : (
          <button
            onClick={handleSignIn}
            className="border-2 border-white text-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition"
          >
            Sign In
          </button>
        )}
      </div>
    </nav>
  );
};

export default NavBar;
