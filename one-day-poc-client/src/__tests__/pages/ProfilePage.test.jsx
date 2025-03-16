import { render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import ProfilePage from '../../pages/ProfilePage';

// Mock fetch
const mockFetch = vi.spyOn(global, 'fetch');

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('displays loading text initially', () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );
  
    // Check for the main loading message
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('displays user data when fetch succeeds', async () => {
    const mockUser = {
      name: 'John Doe',
      email: 'john.doe@example.com',
      dob: '1990-01-01',
      role: 'User',
    };

    mockFetch.mockResolvedValueOnce({
      json: vi.fn().mockResolvedValue(mockUser),
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
      expect(screen.getByDisplayValue('john.doe@example.com')).toBeInTheDocument();
      expect(screen.getByDisplayValue('1990-01-01')).toBeInTheDocument();
    });
  });
});