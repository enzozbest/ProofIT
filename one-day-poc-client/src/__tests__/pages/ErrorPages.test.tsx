import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ErrorRoutes from '../../pages/ErrorPages';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import { AuthProvider } from '@/contexts/AuthContext';
import { ReactNode } from 'react';


beforeEach(() => {
  vi.clearAllMocks();
  vi.resetAllMocks();
});

vi.mock('@/contexts/AuthContext.tsx', () => {
  return {
    useAuth: () => ({ isAuthenticated: false, login: vi.fn() }),
    AuthProvider: ({ children }: { children: ReactNode }) => <>{children}</>,
  };
});

const testCases = [
  {
    path: '/403',
    code: '403',
    message: 'You don’t have permission to access this page.',
  },
  { path: '/401', code: '401', message: 'Please log in to access this page.' },
  { path: '/500', code: '500', message: 'Something went wrong on our end.' },
  {
    path: '/random-path',
    code: '404',
    message: 'The page you are looking for does not exist.',
  },
];



describe('ErrorRoutes', () => {
  testCases.forEach(({ path, code, message }) => {
    it(`renders correct error page for ${path}`, async () => {
      render(
        <MemoryRouter initialEntries={[path]}>
          <AuthProvider>
            <ErrorRoutes />
          </AuthProvider>
        </MemoryRouter>
      );

      expect(screen.getByText(code)).toBeInTheDocument();
      expect(screen.getByText(message)).toBeInTheDocument();
    });
  });

  it('renders fallback error message for unhandled errors', () => {
    render(
      <MemoryRouter initialEntries={['/unknown']}>
        <AuthProvider>
          <ErrorRoutes />
        </AuthProvider>
      </MemoryRouter>
    );

    expect(screen.getByText('404')).toBeInTheDocument();
    expect(
      screen.getByText('The page you are looking for does not exist.')
    ).toBeInTheDocument();
  });
});
