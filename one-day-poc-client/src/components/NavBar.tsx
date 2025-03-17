import React, { FC } from 'react';
import { BotMessageSquare } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const NavBar: React.FC = () => {
  const { isAuthenticated, login, logout } = useAuth();

  return (
    <nav className="absolute top-0 left-0 w-full flex justify-between items-center px-10 py-6 z-10">
      <div className="flex items-center gap-2 text-xl ">
        <BotMessageSquare className="w-6 h-6 " />
        <span className="font-normal">
          PROOF -<span className="font-bold"> IT!</span>
        </span>
      </div>

      <div className="flex gap-2">
        {isAuthenticated ? (
          <button
            onClick={logout}
            className="border-2 border-white bg-transparent  px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition"
          >
            Log Out
          </button>
        ) : (
          <button
            onClick={login}
            className="border-2 border-white  bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition"
          >
            Sign In
          </button>
        )}
      </div>
    </nav>
  );
};

export default NavBar;
