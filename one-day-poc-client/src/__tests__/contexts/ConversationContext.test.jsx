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
import * as FrontEndAPI from '@/api';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/api', () => ({
  fetchChatHistory: vi.fn(),
  createNewConversation: vi.fn(),
  apiUpdateConversationName: vi.fn(),
  getConversationHistory: vi.fn(),
  apiDeleteConversation: vi.fn(),
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
    deleteConversation,
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
      <button
        data-testid="delete-conversation"
        onClick={() => activeConversationId && deleteConversation(activeConversationId)}
      >
        Delete Active Conversation
      </button>
      <button
        data-testid="delete-specific"
        onClick={() => deleteConversation('specific-id')}
      >
        Delete Specific Conversation
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

  test('updateConversationName only updates the specified conversation', async () => {
    const mockConversations = [
      {
        id: 'test-id-1',
        name: 'Conversation 1',
        lastModified: '2023-01-01',
        messageCount: 0,
        messages: [],
      },
      {
        id: 'test-id-2',
        name: 'Conversation 2',
        lastModified: '2023-01-02',
        messageCount: 0,
        messages: [],
      }
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);

    const ConversationStateExposer = () => {
      const { updateConversationName, conversations } = useConversation();
      
      return (
        <div>
          <div data-testid="conversation-1-name">
            {conversations.find(c => c.id === 'test-id-1')?.name || 'not found'}
          </div>
          <div data-testid="conversation-2-name">
            {conversations.find(c => c.id === 'test-id-2')?.name || 'not found'}
          </div>
          <button
            data-testid="update-conversation-1"
            onClick={() => updateConversationName('test-id-1', 'Updated Conversation 1')}
          >
            Update Conversation 1
          </button>
        </div>
      );
    };

    await renderWithAct(
      <ConversationProvider>
        <ConversationStateExposer />
      </ConversationProvider>
    );

    expect(screen.getByTestId('conversation-1-name').textContent).toBe('Conversation 1');
    expect(screen.getByTestId('conversation-2-name').textContent).toBe('Conversation 2');

    const updateButton = screen.getByTestId('update-conversation-1');
    await act(async () => {
      await userEvent.click(updateButton);
    });

    expect(screen.getByTestId('conversation-1-name').textContent).toBe('Updated Conversation 1');
    expect(screen.getByTestId('conversation-2-name').textContent).toBe('Conversation 2');

    expect(FrontEndAPI.apiUpdateConversationName).toHaveBeenCalledWith(
      'test-id-1',
      'Updated Conversation 1'
    );
    
    expect(FrontEndAPI.apiUpdateConversationName).toHaveBeenCalledTimes(1);
  });

  test('loadConversationMessages returns early if conversationId is falsy', async () => {
    const LoadMessagesTestComponent = () => {
      const { setActiveConversationId } = useConversation();
      
      return (
        <div>
          <button 
            data-testid="load-with-empty-string" 
            onClick={() => setActiveConversationId('')}
          >
            Load with empty string
          </button>
          <button 
            data-testid="load-with-null"
            onClick={() => setActiveConversationId(null)}
          >
            Load with null
          </button>
          <button 
            data-testid="load-with-undefined" 
            onClick={() => setActiveConversationId(undefined)}
          >
            Load with undefined
          </button>
        </div>
      );
    };
  
    FrontEndAPI.getConversationHistory.mockClear();
  
    await renderWithAct(
      <ConversationProvider>
        <LoadMessagesTestComponent />
      </ConversationProvider>
    );
  
    const emptyStringButton = screen.getByTestId('load-with-empty-string');
    await act(async () => {
      await userEvent.click(emptyStringButton);
    });
  
    const nullButton = screen.getByTestId('load-with-null');
    await act(async () => {
      await userEvent.click(nullButton);
    });
  
    const undefinedButton = screen.getByTestId('load-with-undefined');
    await act(async () => {
      await userEvent.click(undefinedButton);
    });
  
    expect(FrontEndAPI.getConversationHistory).not.toHaveBeenCalled();
  });

  test('deleteConversation removes the conversation from state and calls API', async () => {
    const mockConversations = [
      {
        id: 'test-id-1',
        name: 'Conversation 1',
        lastModified: '2023-01-01',
        messageCount: 0,
        messages: [],
      },
      {
        id: 'test-id-2',
        name: 'Conversation 2',
        lastModified: '2023-01-02',
        messageCount: 0,
        messages: [],
      }
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);
    FrontEndAPI.apiDeleteConversation.mockResolvedValue(true);

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const setActiveButton = screen.getByTestId('set-active');
    await act(async () => {
      await userEvent.click(setActiveButton);
    });

    expect(screen.getByTestId('conversation-count')).toHaveTextContent('2');
    expect(screen.getByTestId('active-conversation')).toHaveTextContent('test-id');

    const deleteSpecificButton = screen.getByTestId('delete-specific');
    await act(async () => {
      await userEvent.click(deleteSpecificButton);
    });

    expect(FrontEndAPI.apiDeleteConversation).toHaveBeenCalledWith('specific-id');
  });

  test('deleteConversation resets active conversation when deleting the active one', async () => {
    const mockConversations = [
      {
        id: 'test-id',
        name: 'Test Conversation',
        lastModified: '2023-01-01',
        messageCount: 0,
        messages: [],
      }
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);
    FrontEndAPI.apiDeleteConversation.mockResolvedValue(true);

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const setActiveButton = screen.getByTestId('set-active');
    await act(async () => {
      await userEvent.click(setActiveButton);
    });

    expect(screen.getByTestId('active-conversation')).toHaveTextContent('test-id');
    expect(screen.getByTestId('conversation-count')).toHaveTextContent('1');

    const deleteButton = screen.getByTestId('delete-conversation');
    await act(async () => {
      await userEvent.click(deleteButton);
    });

    expect(screen.getByTestId('active-conversation')).toHaveTextContent('No Active Conversation');
    expect(screen.getByTestId('conversation-count')).toHaveTextContent('0');
    expect(screen.getByTestId('messages-count')).toHaveTextContent('0');
    expect(FrontEndAPI.apiDeleteConversation).toHaveBeenCalledWith('test-id');
  });

  test('deleteConversation handles API errors', async () => {
    const mockConversations = [
      {
        id: 'test-id',
        name: 'Test Conversation',
        lastModified: '2023-01-01',
        messageCount: 0,
        messages: [],
      }
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);
    FrontEndAPI.apiDeleteConversation.mockResolvedValue(false);

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const deleteSpecificButton = screen.getByTestId('delete-specific');
    await act(async () => {
      await userEvent.click(deleteSpecificButton);
    });

    expect(screen.getByTestId('conversation-count')).toHaveTextContent('1');
    expect(FrontEndAPI.apiDeleteConversation).toHaveBeenCalledWith('specific-id');
  });

  test('deleteConversation handles exceptions', async () => {
    const mockConversations = [
      {
        id: 'test-id',
        name: 'Test Conversation',
        lastModified: '2023-01-01',
        messageCount: 0,
        messages: [],
      }
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);
    FrontEndAPI.apiDeleteConversation.mockRejectedValue(new Error('API error'));

    await renderWithAct(
      <ConversationProvider>
        <ConversationConsumer />
      </ConversationProvider>
    );

    const deleteSpecificButton = screen.getByTestId('delete-specific');
    await act(async () => {
      await userEvent.click(deleteSpecificButton);
    });

    expect(console.error).toHaveBeenCalledWith(
      'Error deleting conversation:',
      expect.any(Error)
    );
    expect(screen.getByTestId('conversation-count')).toHaveTextContent('1');
  });

  test('deleteConversation sets current conversation to none', async () => {
    const ConversationListTester = () => {
      const { 
        conversations, 
        activeConversationId, 
        setActiveConversationId, 
        deleteConversation 
      } = useConversation();
      
      return (
        <div>
          <div data-testid="active-id">{activeConversationId || 'none'}</div>
          <div data-testid="conversations-count">{conversations.length}</div>
          {conversations.map(conv => (
            <div key={conv.id} data-testid={`conv-${conv.id}`}>
              {conv.name}
              <button 
                data-testid={`select-${conv.id}`}
                onClick={() => setActiveConversationId(conv.id)}
              >
                Select
              </button>
              <button 
                data-testid={`delete-${conv.id}`}
                onClick={() => deleteConversation(conv.id)}
              >
                Delete
              </button>
            </div>
          ))}
        </div>
      );
    };

    const mockConversations = [
      { id: 'conv-1', name: 'First Conv', lastModified: '2023-01-01', messageCount: 0 },
      { id: 'conv-2', name: 'Second Conv', lastModified: '2023-01-02', messageCount: 0 },
      { id: 'conv-3', name: 'Third Conv', lastModified: '2023-01-03', messageCount: 0 }
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);
    FrontEndAPI.apiDeleteConversation.mockResolvedValue(true);
    FrontEndAPI.getConversationHistory.mockResolvedValue([]);

    await renderWithAct(
      <ConversationProvider>
        <ConversationListTester />
      </ConversationProvider>
    );

    expect(screen.getByTestId('conversations-count')).toHaveTextContent('3');
    
    await act(async () => {
      await userEvent.click(screen.getByTestId('select-conv-2'));
    });
    
    expect(screen.getByTestId('active-id')).toHaveTextContent('conv-2');
    
    await act(async () => {
      await userEvent.click(screen.getByTestId('delete-conv-2'));
    });
    
    expect(screen.getByTestId('active-id')).toHaveTextContent('none');
    expect(screen.getByTestId('conversations-count')).toHaveTextContent('2');
    
    expect(screen.queryByTestId('conv-conv-2')).not.toBeInTheDocument();
  });

  test('deleteConversation clears active conversation when deleting the active one', async () => {
    const ConversationListTester = () => {
      const { 
        conversations, 
        activeConversationId, 
        setActiveConversationId, 
        deleteConversation 
      } = useConversation();
      
      return (
        <div>
          <div data-testid="active-id">{activeConversationId || 'none'}</div>
          <div data-testid="conversations-count">{conversations.length}</div>
          {conversations.map(conv => (
            <div key={conv.id} data-testid={`conv-${conv.id}`}>
              {conv.name}
              <button 
                data-testid={`select-${conv.id}`}
                onClick={() => setActiveConversationId(conv.id)}
              >
                Select
              </button>
              <button 
                data-testid={`delete-${conv.id}`}
                onClick={() => deleteConversation(conv.id)}
              >
                Delete
              </button>
            </div>
          ))}
        </div>
      );
    };

    const mockConversations = [
      { id: 'conv-1', name: 'First Conv', lastModified: '2023-01-01', messageCount: 0 },
      { id: 'conv-2', name: 'Second Conv', lastModified: '2023-01-02', messageCount: 0 },
      { id: 'conv-3', name: 'Third Conv', lastModified: '2023-01-03', messageCount: 0 }
    ];

    FrontEndAPI.fetchChatHistory.mockResolvedValue(mockConversations);
    FrontEndAPI.apiDeleteConversation.mockResolvedValue(true);
    FrontEndAPI.getConversationHistory.mockResolvedValue([]);

    await renderWithAct(
      <ConversationProvider>
        <ConversationListTester />
      </ConversationProvider>
    );

    expect(screen.getByTestId('conversations-count')).toHaveTextContent('3');
    
    await act(async () => {
      await userEvent.click(screen.getByTestId('select-conv-2'));
    });
    
    expect(screen.getByTestId('active-id')).toHaveTextContent('conv-2');
    
    await act(async () => {
      await userEvent.click(screen.getByTestId('delete-conv-2'));
    });
    
    expect(screen.getByTestId('active-id')).toHaveTextContent('none');
    expect(screen.getByTestId('conversations-count')).toHaveTextContent('2');
    
    expect(screen.queryByTestId('conv-conv-2')).not.toBeInTheDocument();
  });
});
