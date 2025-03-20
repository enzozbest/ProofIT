import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import ChatMessage from '../../hooks/Chat';
import { sendChatMessage } from '../../api/FrontEndAPI';
import { Message } from '../../types/Types';
import { useConversation } from '../../contexts/ConversationContext';

vi.mock('../../api/FrontEndAPI', () => ({
  sendChatMessage: vi.fn(),
}));

vi.mock('../../contexts/ConversationContext', () => ({
  useConversation: vi.fn(),
}));

describe('ChatMessage Hook', () => {
  const mockSetPrototype = vi.fn();
  const mockSetPrototypeFiles = vi.fn();
  const mockCreateConversation = vi.fn().mockReturnValue('new-conversation-id');

  beforeEach(() => {
    vi.clearAllMocks();

    (useConversation as any).mockReturnValue({
      activeConversationId: 'test-conversation-id',
      createConversation: mockCreateConversation,
      messages: [],
      loadingMessages: false,
    });
  });

  it('should initialize with empty message and empty sentMessages', () => {
    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    expect(result.current.message).toBe('');
    expect(result.current.sentMessages).toEqual([]);
    expect(result.current.errorMessage).toBeNull();
  });

  it('should update sentMessages when messages from context are non-empty', () => {
    const contextMessages: Message[] = [
      {
        role: 'User',
        content: 'Test message from context',
        timestamp: '2023-01-01T12:00:00.000Z',
        conversationId: 'test-conversation-id',
      },
      {
        role: 'LLM',
        content: 'Response from context',
        timestamp: '2023-01-01T12:00:10.000Z',
        conversationId: 'test-conversation-id',
      },
    ];

    const { result, rerender } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    expect(result.current.sentMessages).toEqual([]);

    (useConversation as any).mockReturnValue({
      activeConversationId: 'test-conversation-id',
      createConversation: mockCreateConversation,
      messages: contextMessages,
      loadingMessages: false,
    });

    rerender();

    expect(result.current.sentMessages).toEqual(contextMessages);
  });

  it('should not process empty messages in handleSend', async () => {
    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      await result.current.handleSend('');
    });

    expect(result.current.sentMessages).toEqual([]);
    expect(sendChatMessage).not.toHaveBeenCalled();
  });

  it('should use activeConversationId when available', async () => {
    const mockDate = new Date('2023-01-01T00:00:00Z');
    vi.setSystemTime(mockDate);

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      await result.current.handleSend('Test message');
    });

    expect(result.current.sentMessages[0].conversationId).toBe(
      'test-conversation-id'
    );
    expect(mockCreateConversation).not.toHaveBeenCalled();

    vi.useRealTimers();
  });

  it('should call createConversation when activeConversationId is not available', async () => {
    (useConversation as any).mockReturnValue({
      activeConversationId: '',
      createConversation: mockCreateConversation,
      messages: [],
      loadingMessages: false,
    });

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      await result.current.handleSend('Test message');
    });

    expect(mockCreateConversation).toHaveBeenCalled();
    expect(result.current.sentMessages[0].conversationId).toBe(
      'new-conversation-id'
    );
  });

  it('should accept explicit message parameter in handleSend', async () => {
    const mockDate = new Date('2023-01-01T00:00:00Z');
    vi.setSystemTime(mockDate);

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      // Set a message that should be ignored in favor of the explicit parameter
      result.current.setMessage('This should be ignored');
      await result.current.handleSend('Explicit message');
    });

    expect(result.current.sentMessages[0].content).toBe('Explicit message');
    expect(result.current.message).toBe(''); // Should still clear the message input

    vi.useRealTimers();
  });

  it('should handle chat responses from the API', async () => {
    (sendChatMessage as any).mockImplementation((message, chatCallback) => {
      chatCallback({ message: 'AI response to your message' });
      return Promise.resolve();
    });

    const mockDate = new Date('2023-01-01T00:00:00Z');
    vi.setSystemTime(mockDate);

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      await result.current.handleSend('Test message');
    });

    await waitFor(() => {
      expect(result.current.sentMessages).toHaveLength(2);
    });

    expect(result.current.sentMessages[0]).toEqual({
      role: 'User',
      content: 'Test message',
      timestamp: mockDate.toISOString(),
      conversationId: 'test-conversation-id',
    });

    expect(result.current.sentMessages[1]).toEqual({
      role: 'LLM',
      content: 'AI response to your message',
      timestamp: mockDate.toISOString(),
      conversationId: 'test-conversation-id',
    });

    vi.useRealTimers();
  });

  it('should handle prototype files from the API', async () => {
    const mockPrototypeFiles = [
      { name: 'index.html', content: '<html>...</html>' },
      { name: 'style.css', content: 'body { ... }' },
    ];

    (sendChatMessage as any).mockImplementation(
      (message, chatCallback, prototypeCallback) => {
        prototypeCallback({ files: mockPrototypeFiles });
        return Promise.resolve();
      }
    );

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      await result.current.handleSend('Generate a prototype');
    });

    expect(mockSetPrototype).toHaveBeenCalledWith(true);
    expect(mockSetPrototypeFiles).toHaveBeenCalledWith(mockPrototypeFiles);
  });
  it('should set error message when API call fails', async () => {
    // Mock the sendChatMessage to throw an error
    (sendChatMessage as any).mockRejectedValue(new Error('API Error'));

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      await result.current.handleSend('Test error handling');
    });

    expect(result.current.errorMessage).toBe(
      'Error. Please check your connection and try again.'
    );
  });

  it('should handle empty activeConversationId when creating LLM response', async () => {
    (useConversation as any).mockReturnValue({
      activeConversationId: '',
      createConversation: mockCreateConversation,
      messages: [],
      loadingMessages: false,
    });

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    await act(async () => {
      (sendChatMessage as any).mockImplementation((msg, callback) => {
        callback({ message: 'LLM response' });
        return Promise.resolve();
      });

      await result.current.handleSend('Test message');
    });

    expect(result.current.sentMessages[1].conversationId).toBe('');
  });
});
