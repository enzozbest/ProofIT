import React from "react";

const NavBar = () => {
  return (
    <nav className="flex justify-between items-center w-full p-5 shadow-md border-b">
      <div className="flex items-center gap-2 text-xl font-bold">
        <span className="w-6 h-6">🐱</span> PoCify
      </div>
      <div className="flex gap-2">
        <button className="px-4 py-2 rounded-lg border hover:bg-gray-200 transition">
          Log In
        </button>
        <button className="px-4 py-2 rounded-lg bg-black text-white hover:opacity-80 transition">
          Sign Up
        </button>
      </div>
    </nav>
  );
};

export default NavBar;
