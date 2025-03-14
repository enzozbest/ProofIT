import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

interface AuthContextType {
  isAuthenticated: boolean;
  isAdmin: boolean;
  checkAuth: () => Promise<void>;
  login: (promptText?: string) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{children: ReactNode}> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const navigate = useNavigate();

  const checkAuth = async () => {
    try {
      const response = await fetch('http://localhost:8000/api/auth/check', {
        method: 'GET',
        credentials: 'include',
      });

      if (!response.ok) {
        setIsAuthenticated(false);
        setIsAdmin(false);
        return;
      }

      const data = await response.json();
      if (data.userId) {
        setIsAuthenticated(true);
        setIsAdmin(data.isAdmin || false);

        const savedPrompt = sessionStorage.getItem('selectedPrompt');
        if (savedPrompt) {
          sessionStorage.removeItem('selectedPrompt');
          navigate('/generate', { state: { initialMessage: savedPrompt } });
        }
      } else {
        setIsAuthenticated(false);
        setIsAdmin(false);
      }
    } catch (error) {
      console.error('Authentication check error:', error);
      setIsAuthenticated(false);
      setIsAdmin(false);
    }
  };

  const login = (promptText?: string) => {
    if (promptText) {
      sessionStorage.setItem('selectedPrompt', promptText);
    }
    window.location.href = 'http://localhost:8000/api/auth';
  };

  const logout = async () => {
    try {
      await fetch('http://localhost:8000/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      });
      setIsAuthenticated(false);
      setIsAdmin(false);
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, isAdmin, checkAuth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};