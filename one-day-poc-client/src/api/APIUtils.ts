import UserService from '../services/UserService';

export const API_BASE_URL = 'http://localhost:8000/api';

export const defaultHeaders = {
  'Content-Type': 'application/json',
};

export async function fetchWithAuth(endpoint: string, options: RequestInit = {}) {
  const defaultOptions: RequestInit = {
    method: 'GET',
    credentials: 'include',
    headers: defaultHeaders,
  };

  const mergedOptions = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  return fetch(`${API_BASE_URL}${endpoint}`, mergedOptions);
}

export const handleApiError = (error: unknown, context: string): never => {
  console.error(`Error ${context}:`, error);
  throw error;
};

export const getUserIdOrThrow = (): string => {
  const userId = UserService.getUserId();
  if (!userId) {
    throw new Error('User not logged in');
  }
  return userId;
};