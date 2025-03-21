/* eslint-disable react/prop-types */
import { render, screen, fireEvent } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import NavBar from '@/components/Navbar';
import { useAuth } from '@/contexts/AuthContext';

// Mock the useAuth hook
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

// Mock the Logo component
vi.mock('@/components/Logo', () => ({
  default: () => <div data-testid="logo">AppLogo</div>,
}));

beforeEach(() => {
  vi.resetAllMocks();
});

test('Renders navbar with logo', () => {
  // Mock authentication state
  useAuth.mockReturnValue({
    isAuthenticated: false,
    login: vi.fn(),
    logout: vi.fn(),
  });

  render(<NavBar />);

  // Check if the logo is rendered
  expect(screen.getByTestId('logo')).toBeInTheDocument();

  // Check if the nav element is rendered with correct classes
  const navElement = screen.getByRole('navigation');
  expect(navElement).toHaveClass('fixed top-2 left-0 w-full h-16 flex justify-between items-center px-10 py-6 z-50');
});

test('Renders sign in button when user is not authenticated', () => {
  // Mock authentication state as not authenticated
  useAuth.mockReturnValue({
    isAuthenticated: false,
    login: vi.fn(),
    logout: vi.fn(),
  });

  render(<NavBar />);

  // Check if the sign in button is rendered
  const signInButton = screen.getByRole('button', { name: /sign in/i });
  expect(signInButton).toBeInTheDocument();

  // Check button styling
  expect(signInButton).toHaveClass('border-2 border-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition');

  // Ensure logout button is not present
  expect(screen.queryByRole('button', { name: /log out/i })).not.toBeInTheDocument();
});

test('Renders log out button when user is authenticated', () => {
  // Mock authentication state as authenticated
  useAuth.mockReturnValue({
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  });

  render(<NavBar />);

  // Check if the logout button is rendered
  const logoutButton = screen.getByRole('button', { name: /log out/i });
  expect(logoutButton).toBeInTheDocument();

  // Check button styling
  expect(logoutButton).toHaveClass('border-2 border-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition');

  // Ensure sign in button is not present
  expect(screen.queryByRole('button', { name: /sign in/i })).not.toBeInTheDocument();
});

test('Calls login function when sign in button is clicked', () => {
  // Create mock function for login
  const mockLogin = vi.fn();

  // Mock authentication state
  useAuth.mockReturnValue({
    isAuthenticated: false,
    login: mockLogin,
    logout: vi.fn(),
  });

  render(<NavBar />);

  // Find and click the sign in button
  const signInButton = screen.getByRole('button', { name: /sign in/i });
  fireEvent.click(signInButton);

  // Verify login function was called
  expect(mockLogin).toHaveBeenCalledTimes(1);
});

test('Calls logout function when log out button is clicked', () => {
  // Create mock function for logout
  const mockLogout = vi.fn();

  // Mock authentication state
  useAuth.mockReturnValue({
    isAuthenticated: true,
    login: vi.fn(),
    logout: mockLogout,
  });

  render(<NavBar />);

  // Find and click the logout button
  const logoutButton = screen.getByRole('button', { name: /log out/i });
  fireEvent.click(logoutButton);

  // Verify logout function was called
  expect(mockLogout).toHaveBeenCalledTimes(1);
});