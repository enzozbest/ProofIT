import React, {
  createContext,
  useState,
  useEffect,
  useContext,
  ReactNode,
} from 'react';
import { useNavigate } from 'react-router-dom';
import UserService from '../services/UserService';

interface AuthContextType {
  isAuthenticated: boolean;
  isAdmin: boolean;
  checkAuth: () => Promise<void>;
  login: (promptTextOrEvent?: string | React.MouseEvent, isPredefined?: boolean) => void;
  logout: () => Promise<void>;
}

/**
 * Authentication context type definition
 * Defines the shape of the authentication context data and methods
 */
const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * Authentication Provider Component
 *
 * Manages authentication state and provides authentication-related
 * functionality to child components through React Context.
 *
 * @param {Object} props - Component props
 * @param {ReactNode} props.children - Child components that will have access to auth context
 */
export const AuthProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const navigate = useNavigate();

  const checkAuth = async () => {
    try {
      const response = await fetch('/api/auth/check', {
        method: 'GET',
        credentials: 'include',
      });

      if (!response.ok) {
        setIsAuthenticated(false);
        setIsAdmin(false);
        UserService.clearUser();
        return;
      }

      const data = await response.json();
      if (data.userId) {
        await UserService.fetchUserData();

        setIsAuthenticated(true);
        setIsAdmin(data.isAdmin || false);

        const savedPrompt = sessionStorage.getItem('selectedPrompt');
        if (savedPrompt) {
          const isPredefined = sessionStorage.getItem('isPredefined') === 'true';
          sessionStorage.removeItem('isPredefined');
          sessionStorage.removeItem('selectedPrompt');
          navigate('/generate', { state: { initialMessage: savedPrompt, isPredefined } });
        }
      } else {
        setIsAuthenticated(false);
        setIsAdmin(false);
        UserService.clearUser();
      }
    } catch (error) {
      console.error('Authentication check error:', error);
      setIsAuthenticated(false);
      setIsAdmin(false);
      UserService.clearUser();
    }
  };

  /**
   * Initiates the login process by redirecting to the auth endpoint
   * Optionally saves a prompt text to session storage to be restored after login
   *
   * @param {string | React.MouseEvent} promptTextOrEvent - Optional prompt text to save or click event
   * @param isPredefined
   */
  const login = (promptTextOrEvent?: string | React.MouseEvent, isPredefined?: boolean) => {
    if (promptTextOrEvent && typeof promptTextOrEvent === 'string') {
      sessionStorage.setItem('selectedPrompt', promptTextOrEvent);
      sessionStorage.setItem('isPredefined', String(isPredefined));
    }
    window.location.href = '/api/auth';
  };

  const logout = async () => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'Host': 'proofit.uk',
        },
        body: '{}'
      });
      setIsAuthenticated(false);
      setIsAdmin(false);
      UserService.clearUser();
    } catch (error) {
      //console.error('Logout error:', error);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, isAdmin, checkAuth, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
};

/**
 * Custom hook to access the authentication context
 *
 * Provides easy access to authentication state and methods from any component
 *
 * @returns {AuthContextType} The authentication context value
 * @throws {Error} If used outside of an AuthProvider
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
