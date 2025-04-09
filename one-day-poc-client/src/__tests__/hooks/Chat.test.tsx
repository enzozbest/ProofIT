import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import ChatMessage from '../../hooks/Chat';
import { sendChatMessage } from '@/api';
import { Message, ChatResponse } from '../../types/Types';
import { useConversation } from '../../contexts/ConversationContext';

vi.mock('@/api', () => ({
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
      result.current.setMessage('This should be ignored');
      await result.current.handleSend('Explicit message');
    });

    expect(result.current.sentMessages[0].content).toBe('Explicit message');
    expect(result.current.message).toBe('');

    vi.useRealTimers();
  });

  it('should handle chat responses from the API', async () => {
    (sendChatMessage as any).mockImplementation(
      (
        message: any,
        chatCallback: (response: ChatResponse) => void,
        prototypeCallback?: any,
        isPredefined?: boolean,
        onError?: any
      ) => {
        chatCallback({
          message: 'AI response to your message',
          role: 'LLM',
          timestamp: new Date('2023-01-01T00:00:00Z').toISOString(),
          conversationId: 'test-conversation-id',
          messageId: undefined,
        });
        return Promise.resolve();
      }
    );

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

    expect(result.current.sentMessages[0]).toEqual({
      role: 'User',
      content: 'Test message',
      timestamp: mockDate.toISOString(),
      conversationId: 'test-conversation-id',
    });

    await waitFor(() => {
      expect(result.current.sentMessages.length).toBe(2);
    });

    expect(result.current.sentMessages[1]).toEqual({
      role: 'LLM',
      content: 'AI response to your message',
      timestamp: mockDate.toISOString(),
      conversationId: 'test-conversation-id',
      id: undefined,
      isError: false,
    });

    vi.useRealTimers();
  });

  it('should handle prototype files from the API', async () => {
    const mockPrototypeFiles = [
      { name: 'index.html', content: '<html>...</html>' },
      { name: 'style.css', content: 'body { ... }' },
    ];

    (sendChatMessage as any).mockImplementation(
      (
        message: any,
        chatCallback: any,
        prototypeCallback: (arg0: {
          files: { name: string; content: string }[];
        }) => void
      ) => {
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

  it('should not display chat responses from different conversations', async () => {
    // Mock console.log to track when it's called
    const consoleLogSpy = vi.spyOn(console, 'log');

    // Set up sendChatMessage to return a response from a different conversation
    (sendChatMessage as any).mockImplementation(
      (
        message: any,
        chatCallback: (response: ChatResponse) => void,
        prototypeCallback?: any,
        isPredefined?: boolean,
        onError?: any
      ) => {
        chatCallback({
          message: 'Response from a different conversation',
          role: 'LLM',
          timestamp: new Date().toISOString(),
          conversationId: 'different-conversation-id', // Different from the active one
          messageId: 'different-message-id',
        });
        return Promise.resolve();
      }
    );

    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    // Initial length should be 0
    const initialLength = result.current.sentMessages.length;

    await act(async () => {
      await result.current.handleSend('Test message');
    });

    // Should only have the user message added, not the LLM response
    expect(result.current.sentMessages.length).toBe(initialLength + 1);
    expect(result.current.sentMessages[initialLength].role).toBe('User');

    // Verify the log message about not displaying the message was called
    expect(consoleLogSpy).toHaveBeenCalledWith(
      'Message from different conversation, not displaying',
      {
        messageConversationId: 'different-conversation-id',
        activeConversationId: 'test-conversation-id',
      }
    );

    // Clean up
    consoleLogSpy.mockRestore();
  });

  it('should handle error callback from sendChatMessage', async () => {
    const mockDate = new Date('2023-01-01T00:00:00Z');
    vi.setSystemTime(mockDate);

    (sendChatMessage as any).mockImplementation(
      (
        message: any,
        chatCallback: any,
        prototypeCallback: any,
        isPredefined: boolean = false,
        errorCallback: (errorMsg: string) => void
      ) => {
        errorCallback('Custom error from API');
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
      await result.current.handleSend('Test message with error');
    });

    expect(result.current.sentMessages[0]).toEqual({
      role: 'User',
      content: 'Test message with error',
      timestamp: mockDate.toISOString(),
      conversationId: 'test-conversation-id',
    });

    expect(result.current.sentMessages[1]).toEqual({
      role: 'LLM',
      content: 'Custom error from API',
      timestamp: mockDate.toISOString(),
      conversationId: 'test-conversation-id',
      isError: true,
    });

    expect(result.current.errorMessage).toBe(
      'Error. Please check your connection and try again.'
    );

    vi.useRealTimers();
  });

  it('should apply fallback ID but not display messages from unknown conversations', async () => {
    const chatResponseWithoutId = {
      message: 'Response without conversation ID',
      role: 'LLM',
      timestamp: new Date().toISOString(),
      messageId: 'msg-123',
      conversationId: undefined
    };

    // Setup mocks
    (sendChatMessage as any).mockImplementation(
      (message: any, chatCallback: any) => {
        chatCallback(chatResponseWithoutId);
        return Promise.resolve();
      }
    );

    // Mock different active conversation ID than the one that will be assigned
    (useConversation as any).mockReturnValue({
      activeConversationId: 'different-conversation-id', // Not 'unknown-conversation'
      createConversation: mockCreateConversation,
      messages: [],
      loadingMessages: false,
    });
    
    // Render the hook
    const { result } = renderHook(() =>
      ChatMessage({
        setPrototype: mockSetPrototype,
        setPrototypeFiles: mockSetPrototypeFiles,
      })
    );

    // Trigger message sending
    await act(async () => {
      await result.current.handleSend('Test message');
    });

    // Now check for the message - it should not be found
    const llmMessage = result.current.sentMessages.find(
      msg => msg.role === 'LLM' && msg.content === 'Response without conversation ID'
    );
    expect(llmMessage).toBeUndefined();
  });
});
