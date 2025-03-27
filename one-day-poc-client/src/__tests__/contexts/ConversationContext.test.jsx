import { render, screen } from '@testing-library/react';
import { vi, test, expect, beforeEach, afterEach, describe } from 'vitest';
import userEvent from '@testing-library/user-event';
import {
  ConversationProvider,
  useConversation,
} from '@/contexts/ConversationContext';
import { useAuth } from '@/contexts/AuthContext';
import { act } from 'react-dom/test-utils';
import React from 'react';
import * as FrontEndAPI from '@/api/FrontEndAPI';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/api/FrontEndAPI', () => ({
  fetchChatHistory: vi.fn(),
  createNewConversation: vi.fn(),
  apiUpdateConversationName: vi.fn(),
  getConversationHistory: vi.fn(),
}));

const renderWithAct = async (ui) => {
  await act(async () => {
    render(ui);
  });
};

const ConversationConsumer = () => {
  const {
    conversations,
    activeConversationId,
    messages,
    loadingMessages,
    setActiveConversationId,
    createConversation,
    refreshConversations,
    updateConversationName,
    isLoading,
  } = useConversation();

  return (
    <div>
      <div data-testid="loading-state">
        {isLoading ? 'Loading' : 'Not Loading'}
      </div>
      <div data-testid="messages-loading">
        {loadingMessages ? 'Loading Messages' : 'Messages Not Loading'}
      </div>
      <div data-testid="active-conversation">
        {activeConversationId || 'No Active Conversation'}
      </div>
      <div data-testid="conversation-count">{conversations.length}</div>
      <div data-testid="messages-count">{messages.length}</div>
      <button data-testid="create-conversation" onClick={createConversation}>
        Create Conversation
      </button>
      <button
        data-testid="refresh-conversations"
        onClick={refreshConversations}
      >
        Refresh Conversations
      </button>
      <button
        data-testid="update-name"
        onClick={() =>
          activeConversationId &&
          updateConversationName(activeConversationId, 'Updated Name')
        }
      >
        Update Name
      </button>
      <button
        data-testid="set-active"
        onClick={() => setActiveConversationId('test-id')}
      >
        Set Active
      </button>
    </div>
  );
};

describe('ConversationContext', () => {
  beforeEach(() => {
    vi.resetAllMocks();

    useAuth.mockReturnValue({ isAuthenticated: true });
    FrontEndAPI.fetchChatHistory.mockResolvedValue([]);
    FrontEndAPI.createNewConversation.mockReturnValue('new-conversation-id');
    FrontEndAPI.apiUpdateConversationName.mockResolvedValue(undefined);
    FrontEndAPI.getConversationHistory.mockResolvedValue([]);

    vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('initializes with empty state when authenticated', async () => {
    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    expect(screen.getByTestId('loading-state')).toHaveTextContent(
      'Not Loading'
    );
    expect(screen.getByTestId('active-conversation')).toHaveTextContent(
      'No Active Conversation'
    );
    expect(screen.getByTestId('conversation-count')).toHaveTextContent('0');
    expect(FrontEndAPI.fetchChatHistory).toHaveBeenCalledTimes(1);
  });

  test('clears state when not authenticated', async () => {
    useAuth.mockReturnValue({ isAuthenticated: false });

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    expect(screen.getByTestId('conversation-count')).toHaveTextContent('0');
    expect(screen.getByTestId('active-conversation')).toHaveTextContent(
      'No Active Conversation'
    );
    expect(FrontEndAPI.fetchChatHistory).not.toHaveBeenCalled();
  });

  test('refreshConversations fetches and updates conversations', async () => {
    const mockConversations = [
      {
        id: '1',
        name: 'Test Conversation',
        lastModified: '2023-01-01',
        messageCount: 5,
        messages: [],
      },
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    FrontEndAPI.fetchChatHistory.mockClear();

    const refreshButton = screen.getByTestId('refresh-conversations');

    await act(async () => {
      await userEvent.click(refreshButton);
    });

    expect(FrontEndAPI.fetchChatHistory).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('loading-state')).toHaveTextContent(
      'Not Loading'
    );
    expect(screen.getByTestId('conversation-count')).toHaveTextContent('1');
  });

  test('createConversation adds a new conversation', async () => {
    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const createButton = screen.getByTestId('create-conversation');

    await act(async () => {
      await userEvent.click(createButton);
    });

    expect(FrontEndAPI.createNewConversation).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('conversation-count')).toHaveTextContent('1');
    expect(screen.getByTestId('active-conversation')).toHaveTextContent(
      'new-conversation-id'
    );
  });

  test('updateConversationName updates conversation locally and calls API', async () => {
    const mockConversations = [
      {
        id: 'test-id',
        name: 'Original Name',
        lastModified: '2023-01-01',
        messageCount: 0,
        messages: [],
      },
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const setActiveButton = screen.getByTestId('set-active');
    await act(async () => {
      await userEvent.click(setActiveButton);
    });

    const updateNameButton = screen.getByTestId('update-name');
    await act(async () => {
      await userEvent.click(updateNameButton);
    });

    expect(FrontEndAPI.apiUpdateConversationName).toHaveBeenCalledWith(
      'test-id',
      'Updated Name'
    );
  });

  test('updateConversationName handles API errors', async () => {
    const mockConversations = [
      {
        id: 'test-id',
        name: 'Original Name',
        lastModified: '2023-01-01',
        messageCount: 0,
        messages: [],
      },
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);
    FrontEndAPI.apiUpdateConversationName.mockRejectedValue(
      new Error('Update error')
    );

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const setActiveButton = screen.getByTestId('set-active');
    await act(async () => {
      await userEvent.click(setActiveButton);
    });

    const updateNameButton = screen.getByTestId('update-name');
    await act(async () => {
      await userEvent.click(updateNameButton);
    });

    expect(console.error).toHaveBeenCalledWith(
      'Error updating conversation name:',
      expect.any(Error)
    );
  });

  test('setActiveConversationId loads conversation messages', async () => {
    const mockMessages = [
      { id: 'm1', content: 'Hello', role: 'user', timestamp: '2023-01-01' },
      {
        id: 'm2',
        content: 'Hi there',
        role: 'assistant',
        timestamp: '2023-01-01',
      },
    ];

    FrontEndAPI.getConversationHistory.mockResolvedValue(mockMessages);

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const setActiveButton = screen.getByTestId('set-active');
    await act(async () => {
      await userEvent.click(setActiveButton);
    });

    expect(FrontEndAPI.getConversationHistory).toHaveBeenCalledWith('test-id');
    expect(screen.getByTestId('messages-count')).toHaveTextContent('2');
    expect(screen.getByTestId('active-conversation')).toHaveTextContent(
      'test-id'
    );
  });

  test('setActiveConversationId handles error when loading messages', async () => {
    FrontEndAPI.getConversationHistory.mockRejectedValue(
      new Error('Loading error')
    );

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const setActiveButton = screen.getByTestId('set-active');
    await act(async () => {
      await userEvent.click(setActiveButton);
    });

    expect(console.error).toHaveBeenCalledWith(
      'Error loading conversation messages:',
      expect.any(Error)
    );
    expect(screen.getByTestId('messages-loading')).toHaveTextContent(
      'Messages Not Loading'
    );
  });

  test('useEffect refreshes conversations when authenticated status changes', async () => {
    useAuth.mockReturnValue({ isAuthenticated: false });

    const { rerender } = render(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    expect(FrontEndAPI.fetchChatHistory).not.toHaveBeenCalled();

    await act(async () => {
      useAuth.mockReturnValue({ isAuthenticated: true });
      rerender(
        <ConversationProvider>
          <ConversationConsumer />
        </ConversationProvider>
      );
    });

    expect(FrontEndAPI.fetchChatHistory).toHaveBeenCalledTimes(1);
    expect(console.log).toHaveBeenCalledWith(
      'User authenticated, fetching conversations'
    );
  });

  test('useConversation throws error when used outside provider', () => {
    const originalConsoleError = console.error;
    console.error = vi.fn();

    const ComponentWithoutProvider = () => {
      useConversation();
      return <div>This should never render</div>;
    };

    expect(() => {
      render(<ComponentWithoutProvider />);
    }).toThrow('useConversation must be used within a ConversationProvider');

    console.error = originalConsoleError;
  });
});
