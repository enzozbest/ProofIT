import React from "react";
import { BotMessageSquare } from 'lucide-react'

const NavBar = () => {
  const handleSignIn = () => {
    window.location.href = "http://localhost:8000/api/auth";
  };
  return (
    <nav className="flex justify-between items-center w-full p-5 shadow-md border-b">
      <div className="flex items-center gap-2 text-xl font-bold">
        <BotMessageSquare /> namesy
      </div>
      <div className="flex gap-2">
        <button onClick={handleSignIn}
        className="px-4 py-2 rounded-lg bg-[#213547] text-white hover:opacity-80 transition">
          Sign In
        </button>
      </div>
    </nav>
  );
};

export default NavBar;
