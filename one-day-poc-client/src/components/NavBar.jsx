import React from "react";

const NavBar = () => {
  const handleSignIn = () => {
    window.location.href = "http://localhost:8000/api/auth";
  };
  return (
    <nav className="flex justify-between items-center w-full p-5 shadow-md border-b">
      <div className="flex items-center gap-2 text-xl font-bold">
        <span className="w-6 h-6">🐱</span> PoCify
      </div>
      <div className="flex gap-2">
        <button onClick={handleSignIn}
        className="px-4 py-2 rounded-lg bg-black text-white hover:opacity-80 transition">
          Sign In
        </button>
      </div>
    </nav>
  );
};

export default NavBar;
