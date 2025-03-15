import React, { useEffect, useState } from 'react';
import BackgroundSVG from '../assets/background.svg';

const ProfilePage: React.FC = () => {
  const [user, setUser] = useState<{
    name: string;
    email: string;
    role: string;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('http://localhost:8000/api/auth/me', {
        method: 'GET',
	credentials: 'include'
    })
      .then((response) => response.json())
      .then((response) => {
        setUser(response.data);
        setLoading(false);
      })
      .catch((err) => {
        setError('Failed to load profile');
        setLoading(false);
      });
  }, []);

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
      <div className="flex flex-col items-center justify-center h-screen text-white">
        <h1 className="text-4xl font-bold">Profile</h1>
        <div className="mt-6 p-6 bg-white text-gray-800 rounded-lg shadow-lg w-80 text-center">
          <p className="text-xl font-semibold">{user?.name}</p>
          <p className="text-md text-gray-600">{user?.email}</p>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
