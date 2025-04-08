import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { 
  API_BASE_URL, 
  defaultHeaders, 
  fetchWithAuth, 
  handleApiError, 
  getUserIdOrThrow 
} from '@/api/APIUtils';
import UserService from '@/services/UserService';

vi.mock('@/services/UserService', () => ({
  default: {
    getUserId: vi.fn(),
  },
}));

const originalConsoleError = console.error;

const consoleErrorMock = vi.fn();
console.error = consoleErrorMock;

global.fetch = vi.fn();

describe('API_BASE_URL', () => {
  it('should have the correct value', () => {
    expect(API_BASE_URL).toBe('http://localhost:8000/api');
  });
});

describe('defaultHeaders', () => {
  it('should have the correct content type', () => {
    expect(defaultHeaders).toEqual({
      'Content-Type': 'application/json',
    });
  });
});

describe('fetchWithAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should call fetch with the correct default parameters', async () => {
    (fetch as any).mockResolvedValueOnce({ ok: true });
    
    await fetchWithAuth('/test-endpoint');
    
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/test-endpoint',
      {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );
  });

  it('should merge custom options with defaults', async () => {
    (fetch as any).mockResolvedValueOnce({ ok: true });
    
    const customOptions = {
      method: 'POST',
      body: JSON.stringify({ key: 'value' }),
      headers: {
        'Authorization': 'Bearer token',
      },
    };
    
    await fetchWithAuth('/test-endpoint', customOptions);
    
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/test-endpoint',
      {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({ key: 'value' }),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer token',
        },
      }
    );
  });

  it('should not override Content-Type if specified in custom options', async () => {
    (fetch as any).mockResolvedValueOnce({ ok: true });
    
    const customOptions = {
      headers: {
        'Content-Type': 'application/xml',
      },
    };
    
    await fetchWithAuth('/test-endpoint', customOptions);
    
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/test-endpoint',
      {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/xml',
        },
      }
    );
  });

  it('should return the fetch response', async () => {
    const mockResponse = { 
      ok: true, 
      json: async () => ({ data: 'test' }) 
    };
    (fetch as any).mockResolvedValueOnce(mockResponse);
    
    const result = await fetchWithAuth('/test-endpoint');
    
    expect(result).toBe(mockResponse);
  });
});

describe('handleApiError', () => {
  beforeEach(() => {
    consoleErrorMock.mockClear();
  });

  it('should log the error with context and throw it', () => {
    const testError = new Error('Test error');
    const context = 'testing';
    
    expect(() => handleApiError(testError, context)).toThrow(testError);
  });

  it('should handle non-Error objects', () => {
    const nonErrorObject = { message: 'Not an Error instance' };
    const context = 'object error';
    
    expect(() => handleApiError(nonErrorObject, context)).toThrow();
  });

  it('should handle string errors', () => {
    const stringError = 'String error message';
    const context = 'string error';
    
    expect(() => handleApiError(stringError, context)).toThrow();
  });
});

describe('getUserIdOrThrow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return user ID when available', () => {
    (UserService.getUserId as any).mockReturnValue('test-user-123');
    
    const userId = getUserIdOrThrow();
    
    expect(userId).toBe('test-user-123');
    expect(UserService.getUserId).toHaveBeenCalledTimes(1);
  });

  it('should throw an error when user ID is not available', () => {
    (UserService.getUserId as any).mockReturnValue(null);
    
    expect(() => getUserIdOrThrow()).toThrow('User not logged in');
    expect(UserService.getUserId).toHaveBeenCalledTimes(1);
  });

  it('should throw an error when user ID is empty string', () => {
    (UserService.getUserId as any).mockReturnValue('');
    
    expect(() => getUserIdOrThrow()).toThrow('User not logged in');
    expect(UserService.getUserId).toHaveBeenCalledTimes(1);
  });
});

afterEach(() => {
  console.error = originalConsoleError;
});