import { describe, it, vi, expect, afterEach, beforeEach } from 'vitest';
import { sendChatMessage } from '@/api/FrontEndAPI';
import { Message, ChatResponse, PrototypeResponse } from '@/types/Types';
import UserService from '@/services/UserService';
import { v4 as uuidv4 } from 'uuid';

vi.mock('uuid', () => ({
  v4: () => 'mocked-uuid-value',
}));

vi.mock('@/services/UserService', () => ({
  default: {
    getUserId: vi.fn().mockReturnValue('anonymous'),
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
