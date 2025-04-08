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
  getConversationHistory,
} from '@/api/MessageAPI';
import {
  Message,
  ChatResponse,
  PrototypeResponse,
} from '@/types/Types';
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

// Need to mock the createNewConversation function which is imported from ConversationAPI
vi.mock('@/api/ConversationAPI', () => ({
  createNewConversation: () => 'mocked-uuid-value',
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
      predefined: false,
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
      sendChatMessage(mockMessage, vi.fn(), vi.fn(), false, onError)
    ).rejects.toThrow(errorText);

    expect(onError).toHaveBeenCalledWith('There was an error, please try again');
    expect(onError).toHaveBeenCalledTimes(2);
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

afterAll(() => {
  console.error = originalConsoleError;
  console.log = originalConsoleLog;
});