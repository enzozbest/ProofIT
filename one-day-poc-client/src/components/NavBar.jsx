import { BotMessageSquare } from 'lucide-react'

const NavBar = ({isAuthenticated, setIsAuthenticated}) => {
  const handleSignIn = () => {
    window.location.href = "http://localhost:8000/api/auth";
  };

  const handleSignOut = () => {
    fetch("http://localhost:8000/auth/logout", {
      method: "POST",
      credentials: "include",
    })
      .then(() => {
        setIsAuthenticated(false);
      })
      .catch((error) => console.error("Error:", error));
  };

  return (
    <nav className="flex justify-between items-center w-full p-5 shadow-md border-b">
      <div className="flex items-center gap-2 text-xl font-bold">
        <BotMessageSquare /> ProofIt!
      </div>
      <div className="flex gap-2">
        {isAuthenticated ? (
          <button onClick={handleSignOut} 
            className="px-4 py-2 rounded-lg bg-[#213547] text-white hover:opacity-80 transition">
            Log Out
          </button>
        ) : (
          <button onClick={handleSignIn}
          className="px-4 py-2 rounded-lg bg-[#213547] text-white hover:opacity-80 transition">
          Sign In
          </button>
        )}
      </div>
    </nav>
  );
};

export default NavBar;
