import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter, useNavigate } from 'react-router-dom';
import ProfilePage from '../../pages/ProfilePage';
import UserService from '../../services/UserService';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '@/contexts/AuthContext';
import { ReactNode } from 'react';

const mockNavigate = vi.fn();

vi.mock('../../services/UserService', () => ({
  default: {
    fetchUserData: vi.fn(),
    getError: vi.fn(),
    clearUser: vi.fn(),
  },
}));

vi.mock('@/contexts/AuthContext.tsx', () => {
  return {
    useAuth: () => ({ isAuthenticated: false, login: vi.fn() }),
    AuthProvider: ({ children }: { children: ReactNode }) => <>{children}</>,
  };
});

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('displays loading text initially', () => {
    render(
        <MemoryRouter>
          <AuthProvider>
            <ProfilePage />
          </AuthProvider>
        </MemoryRouter>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('displays user data when fetch succeeds', async () => {
    const mockUser = {
      name: 'John Doe',
      email: 'john.doe@example.com',
      dob: '1990-01-01',
      role: 'User',
    };

    vi.spyOn(UserService, 'fetchUserData').mockResolvedValueOnce(mockUser);
    vi.spyOn(UserService, 'getError').mockReturnValue(null);

    render(
      <MemoryRouter>
        <AuthProvider>
          <ProfilePage />
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
      expect(
        screen.getByDisplayValue('john.doe@example.com')
      ).toBeInTheDocument();
      expect(screen.getByDisplayValue('1990-01-01')).toBeInTheDocument();
    });
  });

  it('displays an error message when fetch fails', async () => {
    vi.spyOn(UserService, 'fetchUserData').mockResolvedValueOnce(null);
    vi.spyOn(UserService, 'getError').mockReturnValue(
      'Failed to load user data'
    );

    render(
        <MemoryRouter>
          <AuthProvider>
            <ProfilePage />
          </AuthProvider>
        </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Failed to load user data')).toBeInTheDocument();
    });
  });

  it('displays default error message when no error is returned', async () => {
    vi.spyOn(UserService, 'fetchUserData').mockResolvedValueOnce(null);
    vi.spyOn(UserService, 'getError').mockReturnValue(null);

    render(
        <MemoryRouter>
          <AuthProvider>
            <ProfilePage />
          </AuthProvider>
        </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Failed to load profile')).toBeInTheDocument();
    });
  });

  it('navigates back when the arrow button is clicked', async () => {
    vi.mocked(UserService.fetchUserData).mockResolvedValue({
      name: 'John Doe',
      email: 'john.doe@example.com',
      dob: '1990-01-01',
      role: 'User',
    });

    render(
        <MemoryRouter>
          <AuthProvider>
            <ProfilePage />
          </AuthProvider>
        </MemoryRouter>
    );

    await waitFor(() =>
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument()
    );

    const backButton = screen.getByTestId('back-button');
    await userEvent.click(backButton);

    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });
});
