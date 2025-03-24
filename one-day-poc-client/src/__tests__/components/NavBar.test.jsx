/* eslint-disable react/prop-types */
import { render, screen, fireEvent } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import NavBar from '@/components/NavBar';
import { useAuth } from '@/contexts/AuthContext';
import { MemoryRouter } from 'react-router-dom';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/components/Logo', () => ({
  default: () => <div data-testid="logo">AppLogo</div>,
}));

beforeEach(() => {
  vi.resetAllMocks();
});

test('Renders navbar with logo', () => {
  useAuth.mockReturnValue({
    isAuthenticated: false,
    login: vi.fn(),
    logout: vi.fn(),
  });

  render(
    <MemoryRouter>
      <NavBar />
    </MemoryRouter>);

  expect(screen.getByTestId('logo')).toBeInTheDocument();

  const navElement = screen.getByRole('navigation');
  expect(navElement).toHaveClass(
    'fixed top-2 left-0 w-full h-16 flex justify-between items-center px-10 py-6 z-50'
  );
});

test('Renders sign in button when user is not authenticated', () => {
  useAuth.mockReturnValue({
    isAuthenticated: false,
    login: vi.fn(),
    logout: vi.fn(),
  });

  render(<MemoryRouter>
    <NavBar />
  </MemoryRouter>);

  const signInButton = screen.getByRole('button', { name: /sign in/i });
  expect(signInButton).toBeInTheDocument();

  expect(signInButton).toHaveClass(
    'border-2 border-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition'
  );

  expect(
    screen.queryByRole('button', { name: /log out/i })
  ).not.toBeInTheDocument();
});

test('Renders log out button when user is authenticated', () => {
  useAuth.mockReturnValue({
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  });

  render(<MemoryRouter>
    <NavBar />
  </MemoryRouter>);

  const logoutButton = screen.getByRole('button', { name: /log out/i });
  expect(logoutButton).toBeInTheDocument();

  expect(logoutButton).toHaveClass(
    'border-2 border-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition'
  );

  expect(
    screen.queryByRole('button', { name: /sign in/i })
  ).not.toBeInTheDocument();
});

test('Calls login function when sign in button is clicked', () => {
  const mockLogin = vi.fn();

  useAuth.mockReturnValue({
    isAuthenticated: false,
    login: mockLogin,
    logout: vi.fn(),
  });

  render(<MemoryRouter>
    <NavBar />
  </MemoryRouter>
  );

  const signInButton = screen.getByRole('button', { name: /sign in/i });
  fireEvent.click(signInButton);

  expect(mockLogin).toHaveBeenCalledTimes(1);
});

test('Calls logout function when log out button is clicked', () => {
  const mockLogout = vi.fn();

  useAuth.mockReturnValue({
    isAuthenticated: true,
    login: vi.fn(),
    logout: mockLogout,
  });

  render(<MemoryRouter>
    <NavBar />
  </MemoryRouter>);

  const logoutButton = screen.getByRole('button', { name: /log out/i });
  fireEvent.click(logoutButton);

  expect(mockLogout).toHaveBeenCalledTimes(1);
});
