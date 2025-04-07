import UserService from '@/services/UserService';
import { vi, describe, test, expect, beforeEach, afterEach } from 'vitest';

interface User {
  name: string;
  email: string;
  dob: string;
  role: string;
  id?: string;
}

describe('UserService', () => {
  beforeEach(() => {
    UserService.clearUser();

    vi.restoreAllMocks();

    vi.spyOn(window, 'fetch');

    vi.spyOn(console, 'log').mockImplementation(() => {});
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('should behave as singleton', () => {
    const mockUser: User = {
      name: 'Singleton Test User',
      email: 'singleton@example.com',
      dob: '1990-01-01',
      role: 'user',
    };

    UserService.setUser(mockUser);

    const anotherServiceReference = UserService;

    expect(anotherServiceReference.getUser()).toEqual(mockUser);

    UserService.clearUser();
  });

  test('fetchUserData should get user data from API', async () => {
    const mockUser: User = {
      name: 'Test User',
      email: 'test@example.com',
      dob: '1990-01-01',
      role: 'user',
      id: '123',
    };

    vi.spyOn(window, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockUser),
    } as Response);

    const user = await UserService.fetchUserData();

    expect(window.fetch).toHaveBeenCalledWith(
      'https://proofit.uk/api/auth/me',
      {
        method: 'GET',
        credentials: 'include',
      }
    );

    expect(user).toEqual(mockUser);
    expect(UserService.getUser()).toEqual(mockUser);
    expect(UserService.isLoading()).toBe(false);
    expect(UserService.getError()).toBe(null);
    expect(console.log).toHaveBeenCalledWith('Fetched user data');
  });

  test('fetchUserData should return cached user if available', async () => {
    const mockUser: User = {
      name: 'Cached User',
      email: 'cached@example.com',
      dob: '1990-01-01',
      role: 'user',
      id: '456',
    };

    UserService.setUser(mockUser);

    const user = await UserService.fetchUserData();

    expect(window.fetch).not.toHaveBeenCalled();
    expect(user).toEqual(mockUser);
  });

  test('fetchUserData should handle API errors', async () => {
    vi.spyOn(window, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
    } as Response);

    const user = await UserService.fetchUserData();

    expect(user).toBe(null);
    expect(UserService.getError()).toBe('Failed to load user data');
    expect(UserService.isLoading()).toBe(false);
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching user data:',
      expect.any(Error)
    );
  });

  test('fetchUserData should handle network errors', async () => {
    const networkError = new Error('Network failure');
    vi.spyOn(window, 'fetch').mockRejectedValueOnce(networkError);

    const user = await UserService.fetchUserData();

    expect(user).toBe(null);
    expect(UserService.getError()).toBe('Network failure');
    expect(UserService.isLoading()).toBe(false);
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching user data:',
      networkError
    );
  });

  test('fetchUserData should handle non-Error exceptions', async () => {
    vi.spyOn(window, 'fetch').mockRejectedValueOnce('String error'); // Not an Error object

    const user = await UserService.fetchUserData();

    expect(user).toBe(null);
    expect(UserService.getError()).toBe('Unknown error');
    expect(UserService.isLoading()).toBe(false);
  });

  test('setUser should update the current user', () => {
    const mockUser: User = {
      name: 'New User',
      email: 'new@example.com',
      dob: '1995-05-05',
      role: 'admin',
      id: '789',
    };

    UserService.setUser(mockUser);

    expect(UserService.getUser()).toEqual(mockUser);
  });

  test('getUserId should return id if available', () => {
    const mockUser: User = {
      name: 'User with ID',
      email: 'withid@example.com',
      dob: '1995-05-05',
      role: 'user',
      id: 'user-id-123',
    };

    UserService.setUser(mockUser);

    expect(UserService.getUserId()).toBe('user-id-123');
  });

  test('getUserId should fall back to email if id is not available', () => {
    const mockUser: User = {
      name: 'User without ID',
      email: 'noid@example.com',
      dob: '1995-05-05',
      role: 'user',
    };

    UserService.setUser(mockUser);

    expect(UserService.getUserId()).toBe('noid@example.com');
  });

  test('getUserId should return "anonymous" if no user is set', () => {
    UserService.clearUser();

    expect(UserService.getUserId()).toBe('anonymous');
  });

  test('clearUser should remove the current user', () => {
    const mockUser: User = {
      name: 'User to clear',
      email: 'clear@example.com',
      dob: '1995-05-05',
      role: 'user',
    };

    UserService.setUser(mockUser);

    UserService.clearUser();

    expect(UserService.getUser()).toBe(null);
  });
});
