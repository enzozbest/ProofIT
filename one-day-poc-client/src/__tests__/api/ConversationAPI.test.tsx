import {
  describe,
  it,
  vi,
  expect,
  afterEach,
  beforeEach,
  afterAll,
} from 'vitest';
import {
  fetchChatHistory,
  createNewConversation,
  apiUpdateConversationName,
  apiDeleteConversation,
} from '@/api/ConversationAPI';
import { Conversation } from '@/types/Types';
import UserService from '@/services/UserService';

const originalConsoleError = console.error;
const originalConsoleLog = console.log;
console.error = vi.fn();
console.log = vi.fn();

vi.mock('uuid', () => ({
  v4: () => 'mocked-uuid-value',
}));

vi.mock('@/services/UserService', () => ({
  default: {
    getUserId: vi.fn().mockReturnValue('anonymous'),
    getUser: vi.fn().mockReturnValue({ id: 'user-123' }),
  },
}));

global.fetch = vi.fn();

// Test createNewConversation
describe('createNewConversation', () => {
  it('should return a UUID', () => {
    const result = createNewConversation();
    expect(result).toBe('mocked-uuid-value');
  });
});

// Tests for apiUpdateConversationName
describe('apiUpdateConversationName', () => {
  const consoleErrorSpy = vi.spyOn(console, 'error');

  beforeEach(() => {
    vi.clearAllMocks();
    consoleErrorSpy.mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should make a POST request to the correct endpoint with the provided name', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
      })
    );

    const conversationId = 'test-conversation-123';
    const newName = 'Updated Conversation Name';

    await apiUpdateConversationName(conversationId, newName);

    expect(fetch).toHaveBeenCalledWith(
      `http://localhost:8000/api/chat/json/${conversationId}/rename`,
      {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newName }),
      }
    );

    expect(consoleErrorSpy).not.toHaveBeenCalled();
  });
  
  it('should not throw or crash when both parameters are empty strings', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true }));

    await expect(apiUpdateConversationName('', '')).resolves.not.toThrow();

    expect(fetch).toHaveBeenCalled();
  });
  
  it('should work with empty name string', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true }));

    await apiUpdateConversationName('test-conversation', '');

    expect(fetch).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({
        body: JSON.stringify({ name: '' }),
      })
    );
  });

  it('should log an error message when the response is not ok', async () => {
    (fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
    });

    await apiUpdateConversationName('test-conversation', 'New Name');

    expect(console.error).toHaveBeenCalledWith(
      'Failed to update conversation name'
    );
  });

  it('should log network errors with the error object', async () => {
    const networkError = new Error('Network failure');

    (fetch as any).mockRejectedValueOnce(networkError);

    await apiUpdateConversationName('test-conversation', 'New Name');

    expect(console.error).toHaveBeenCalledWith(
      'Error updating conversation name:',
      networkError
    );
  });
});

// Tests for apiDeleteConversation
describe('apiDeleteConversation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (console.error as any).mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should return false when the response is not ok', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
      })
    );

    const conversationId = 'test-conversation-123';
    const result = await apiDeleteConversation(conversationId);

    expect(result).toBe(false);
    expect(console.error).toHaveBeenCalledWith(
      'Failed to delete conversation'
    );
  });

  it('should handle network errors', async () => {
    const networkError = new Error('Network failure');
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(networkError));

    const conversationId = 'test-conversation-123';
    const result = await apiDeleteConversation(conversationId);

    expect(result).toBe(false);
    expect(console.error).toHaveBeenCalledWith(
      'Error deleting conversation:',
      networkError
    );
  });
});

// Tests for fetchChatHistory
describe('fetchChatHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (console.error as any).mockClear();
    (console.log as any).mockClear();

    (UserService.getUser as any).mockReturnValue({ id: 'user-123' });
    (UserService.getUserId as any).mockReturnValue('user-123');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should fetch chat history from the correct API endpoint with user ID', async () => {
    const mockConversations = [
      {
        id: 'conv-1',
        name: 'Conversation 1',
        createdAt: '2023-01-01T12:00:00Z',
        userId: 'user-123',
      },
      {
        id: 'conv-2',
        name: 'Conversation 2',
        createdAt: '2023-01-02T12:00:00Z',
        userId: 'user-123',
      },
    ];

    const mockResponse = {
      conversations: mockConversations,
    };

    (fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    const result = await fetchChatHistory();

    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/chat/history?userId=user-123',
      {
        method: 'GET',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    expect(console.log).toHaveBeenCalledWith('Fetching chat history');

    expect(result).toEqual(mockConversations);
  });

  it('should return empty array when user is not logged in', async () => {
    (UserService.getUser as any).mockReturnValueOnce(null);

    const result = await fetchChatHistory();

    expect(result).toEqual([]);

    expect(console.log).toHaveBeenCalledWith(
      'Cannot fetch chat history as user is not logged in'
    );

    expect(fetch).not.toHaveBeenCalled();
  });

  it('should handle API error responses', async () => {
    (fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    });

    const result = await fetchChatHistory();

    expect(result).toEqual([]);

    expect(console.error).toHaveBeenCalledWith(
      'Error fetching chat history:',
      expect.any(Error)
    );
  });
});

afterAll(() => {
  console.error = originalConsoleError;
  console.log = originalConsoleLog;
});