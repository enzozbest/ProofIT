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
  sendChatMessage,
  fetchChatHistory,
  apiUpdateConversationName,
  getConversationHistory,
} from '@/api/FrontEndAPI';
import {
  Message,
  ChatResponse,
  PrototypeResponse,
  Conversation,
} from '@/types/Types';
import UserService from '@/services/UserService';
import { getPrototypeForMessage } from '@/api/FrontEndAPI';

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

describe('sendChatMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should call onChatResponse and onPrototypeResponse when the API call is successful', async () => {
    const mockMessage: Message = {
      role: 'User',
      content: 'Test message',
      timestamp: new Date().toISOString(),
    };

    const mockChatResponse: ChatResponse = {
      message: 'Chat response text',
      role: 'LLM',
      timestamp: new Date().toISOString(),
    };
    const mockPrototypeResponse: PrototypeResponse = {
      files: {},
    };

    const mockServerResponse = {
      chat: mockChatResponse,
      prototype: mockPrototypeResponse,
    };

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockServerResponse,
      })
    );

    const onChatResponse = vi.fn();
    const onPrototypeResponse = vi.fn();

    await sendChatMessage(mockMessage, onChatResponse, onPrototypeResponse);

    expect(onChatResponse).toHaveBeenCalledWith(mockChatResponse);
    expect(onPrototypeResponse).toHaveBeenCalledWith(mockPrototypeResponse);

    expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/chat/json', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: expect.stringContaining(mockMessage.content),
    });

    const callArgs = (fetch as any).mock.calls[0][1];
    const bodyContent = JSON.parse(callArgs.body);

    expect(bodyContent).toEqual({
      userID: 'anonymous',
      time: mockMessage.timestamp,
      prompt: mockMessage.content,
      conversationId: 'mocked-uuid-value',
    });
  });

  it('should throw an error when the network response is not ok', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false }));

    const mockMessage: Message = {
      role: 'User',
      content: 'Test message',
      timestamp: new Date().toISOString(),
    };

    await expect(
      sendChatMessage(mockMessage, vi.fn(), vi.fn())
    ).rejects.toThrow('Network response was not ok');

    vi.restoreAllMocks();
  });

  it('should throw an error when fetch fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('Fetch failed'))
    );

    const mockMessage: Message = {
      role: 'User',
      content: 'Test message',
      timestamp: new Date().toISOString(),
    };

    await expect(
      sendChatMessage(mockMessage, vi.fn(), vi.fn())
    ).rejects.toThrow('Fetch failed');

    vi.restoreAllMocks();
  });
});

describe('getPrototypeForMessage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (console.error as any).mockClear();
    (console.log as any).mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should return null and log error if either conversationId or messageId is missing', async () => {
    const result1 = await getPrototypeForMessage('', 'some-message-id');
    expect(result1).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: '', messageId: 'some-message-id' }
    );

    (console.error as any).mockClear();

    const result2 = await getPrototypeForMessage('some-conversation-id', '');
    expect(result2).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: 'some-conversation-id', messageId: '' }
    );
  });

  it('should fetch and return the file tree on successful API call', async () => {
    const mockFileTree = { 'file1.ts': '// content', 'file2.jsx': '<Component />' };
    const mockApiResponse = {
      files: JSON.stringify(mockFileTree)
    };

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockApiResponse,
      })
    );

    const result = await getPrototypeForMessage('valid-conversation-id', 'valid-message-id');

    expect(result).toEqual(mockFileTree);
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/chat/history/valid-conversation-id/valid-message-id',
      {
        method: 'GET',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    expect(console.log).toHaveBeenCalledWith('Fetching prototype with:', {
      conversationId: 'valid-conversation-id',
      messageId: 'valid-message-id'
    });

    expect(console.log).toHaveBeenCalledWith('Successfully fetched prototype for:', {
      conversationId: 'valid-conversation-id',
      messageId: 'valid-message-id'
    });
  });

  it('should return null when API response is not ok', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
      })
    );

    const result = await getPrototypeForMessage('invalid-conversation-id', 'invalid-message-id');

    expect(result).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Request failed for:',
      { conversationId: 'invalid-conversation-id', messageId: 'invalid-message-id' },
      'Status:',
      404
    );
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: 'invalid-conversation-id', messageId: 'invalid-message-id' }
    );
  });

  it('should return null when network error occurs', async () => {
    const networkError = new Error('Network failure');
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(networkError));

    const result = await getPrototypeForMessage('test-conversation-id', 'test-message-id');

    expect(result).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      networkError,
      'For IDs:',
      { conversationId: 'test-conversation-id', messageId: 'test-message-id' }
    );
  });

  it('should handle parsing errors in the response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ files: 'not-valid-json' }), // This will cause JSON.parse to throw
      })
    );

    const result = await getPrototypeForMessage('valid-id', 'valid-msg-id');

    expect(result).toBeNull();
    expect(console.error).toHaveBeenCalledWith(
      'Error fetching prototype:',
      expect.any(Error),
      'For IDs:',
      { conversationId: 'valid-id', messageId: 'valid-msg-id' }
    );
  });

  it('should extract error data and call onError for 500 status responses', async () => {
    const errorText = 'Server internal error details';
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        text: async () => errorText,
      })
    );

    const mockMessage: Message = {
      role: 'User',
      content: 'Test message',
      timestamp: new Date().toISOString(),
    };

    const onError = vi.fn();

    await expect(
      sendChatMessage(mockMessage, vi.fn(), vi.fn(), onError)
    ).rejects.toThrow(errorText);

    expect(onError).toHaveBeenCalledWith('There was an error, please try again');
    vi.restoreAllMocks();
  });

  it('should not call onError if not provided for 500 status', async () => {
    const errorText = 'Server internal error details';
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        text: async () => errorText,
      })
    );

    const mockMessage: Message = {
      role: 'User',
      content: 'Test message',
      timestamp: new Date().toISOString(),
    };

    await expect(
      sendChatMessage(mockMessage, vi.fn(), vi.fn())
    ).rejects.toThrow(errorText);

    expect(console.error).toHaveBeenCalledWith('API Error:', expect.any(Error));
    vi.restoreAllMocks();
  });
});

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

describe('getConversationHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (console.error as any).mockClear();
    (console.log as any).mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should fetch conversation history from the correct API endpoint', async () => {
    const conversationId = 'test-conversation-123';
    const mockMessages = [
      {
        id: 'msg-1',
        content: 'Hello, how can I help you?',
        senderId: 'llm',
        timestamp: '2023-01-01T12:00:00Z',
        conversationId: conversationId,
      },
      {
        id: 'msg-2',
        content: 'I need help with coding',
        senderId: 'user',
        timestamp: '2023-01-01T12:01:00Z',
        conversationId: conversationId,
      },
    ];

    (fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockMessages,
    });

    const result = await getConversationHistory(conversationId);

    expect(fetch).toHaveBeenCalledWith(
      `http://localhost:8000/api/chat/history/${conversationId}`,
      {
        method: 'GET',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    expect(console.log).toHaveBeenCalledWith(
      'Received message data:',
      mockMessages
    );

    expect(result).toHaveLength(2);
    expect(result[0].role).toBe('LLM');
    expect(result[1].role).toBe('User');

    expect(result[0]).toEqual({
      ...mockMessages[0],
      role: 'LLM',
    });
    expect(result[1]).toEqual({
      ...mockMessages[1],
      role: 'User',
    });
  });

  it('should handle API error responses', async () => {
    const conversationId = 'invalid-conversation';

    (fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 404,
      statusText: 'Not Found',
    });

    const result = await getConversationHistory(conversationId);

    expect(result).toEqual([]);

    expect(console.error).toHaveBeenCalledWith(
      'Error fetching conversation history:',
      expect.any(Error)
    );
  });

  it('should handle network errors', async () => {
    const networkError = new Error('Network failure');

    (fetch as any).mockRejectedValueOnce(networkError);

    const result = await getConversationHistory('test-conversation');

    expect(result).toEqual([]);

    expect(console.error).toHaveBeenCalledWith(
      'Error fetching conversation history:',
      networkError
    );
  });

  it('should return an empty array when messages is not an array', async () => {
    const conversationId = 'test-conversation-123';

    const nonArrayResponses = [
      null,
      undefined,
      {},
      { messages: 'not an array' },
      123,
      'string response',
      true,
    ];

    for (const nonArrayResponse of nonArrayResponses) {
      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => nonArrayResponse,
      });

      const result = await getConversationHistory(conversationId);

      expect(result).toEqual([]);

      expect(console.log).toHaveBeenCalledWith(
        'Received message data:',
        nonArrayResponse
      );

      (console.log as any).mockClear();
    }
  });
});

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
