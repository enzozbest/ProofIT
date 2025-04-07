import React, { useEffect, useState } from 'react';
import BackgroundSVG from '../assets/background.svg';
import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import UserService from '../services/UserService';
import NavBar from '@/components/NavBar';
import { useAuth } from '@/contexts/AuthContext';


/**
 * ProfilePage Component
 *
 * Displays user profile information including:
 * - Name
 * - Email address
 * - Date of birth
 *
 * The component handles data loading states, error conditions, and provides
 * a back navigation button. All form fields are displayed as read-only.
 *
 * @component
 * @returns {JSX.Element} A profile page with user details or appropriate loading/error states
 */
const ProfilePage: React.FC = () => {
  const [user, setUser] = useState<{
    name: string;
    email: string;
    dob: string;
    role: string;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    async function loadUserData() {
      setLoading(true);
      const userData = await UserService.fetchUserData();
      if (userData) {
        setUser(userData);
        setError(null);
      } else {
        setError(UserService.getError() || 'Failed to load profile');
      }
      setLoading(false);
    }

    loadUserData();
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/403');
    }
  }, [isAuthenticated, navigate]);

  if (loading)
    return (
      <div className="flex items-center justify-center h-screen text-white">
        Loading...
      </div>
    );
  if (error)
    return (
      <div className="flex items-center justify-center h-screen text-white">
        {error}
      </div>
    );

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
      <div className="flex flex-col items-center justify-center h-screen text-white">
        <div className="relative w-80 mb-4 flex justify-center items-center">
          <ArrowLeft
            size={35}
            data-testid="back-button"
            className="absolute left-0 top-1.5 cursor-pointer"
            onClick={() => navigate(-1)}
          />
          <h1 className="text-4xl font-bold">Profile</h1>
        </div>
        <div className="mt-6 p-6 bg-white bg-opacity-80 text-gray-800 rounded-lg shadow-lg w-80 text-center">
          <div className="mb-4 text-left">
            <label className="block text-gray-700 text-sm mb-1">Name</label>
            <input
              type="text"
              value={user?.name}
              className="w-full p-3 bg-gray-200 text-gray-400 border border-gray-300 rounded-lg focus:outline-none"
              disabled
            />
          </div>

          <div className="mb-4 text-left">
            <label className="block text-gray-700 text-sm mb-1">
              Email address
            </label>
            <input
              type="email"
              value={user?.email}
              className="w-full p-3 bg-gray-200 text-gray-400 border border-gray-300 rounded-lg focus:outline-none"
              disabled
            />
          </div>

          <div className="text-left">
            <label className="block text-gray-700 text-sm mb-1">
              Date of Birth
            </label>
            <input
              type="text"
              value={user?.dob}
              className="w-full p-3 bg-gray-200 text-gray-400 border border-gray-300 rounded-lg focus:outline-none"
              disabled
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
