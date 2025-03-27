import { render, act } from '@testing-library/react';
import { describe, it, vi, expect, beforeEach, afterEach } from 'vitest';
import { toast } from 'sonner';
import ChatScreen from '@/components/chat/ChatScreen';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '@/contexts/AuthContext';
import { screen } from '@testing-library/react';
import { FileTree } from '@/types/Types';
import { ConversationProvider } from '@/contexts/ConversationContext';

let capturedOnLoadPrototype: any = null;
let mockSetErrorMessage = vi.fn();

interface Message {
  id: string;
  role: string;
  content: string;
  timestamp: string;
  isError?: boolean;
}

const defaultMocks = {
  chat: {
    message: '',
    setMessage: vi.fn(),
    sentMessages: [] as Message[],
    handleSend: vi.fn(),
    errorMessage: '',
    setErrorMessage: mockSetErrorMessage, 
  },
  conversation: {
    messages: [] as Message[],
    loadingMessages: false,
    activeConversationId: 'test-id'
  }
};

let capturedMessages: any[] = [];

vi.mock('@/components/chat/MessagesBox', () => ({
  MessageBox: ({ sentMessages, onLoadPrototype }: { 
    sentMessages: any[], 
    onLoadPrototype: (files: FileTree) => void 
  }) => {
    capturedOnLoadPrototype = onLoadPrototype;
    capturedMessages = sentMessages;
    
    return (
      <div data-testid="message-box">
        {sentMessages.map((msg: any, index: number) => (
          <div key={index} data-testid={`message-${msg.id || index}`}>
            {msg.content}
          </div>
        ))}
      </div>
    );
  }
}));

vi.mock('@/contexts/ConversationContext', () => ({
  useConversation: () => defaultMocks.conversation,
  ConversationProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>
}));

vi.mock('@/hooks/Chat', () => ({
  __esModule: true,
  default: () => defaultMocks.chat
}));

const mockSessionStorage = {
  removeItem: vi.fn(),
};

Object.defineProperty(window, 'sessionStorage', {
  value: mockSessionStorage,
  writable: true
});

beforeEach(() => {
  capturedOnLoadPrototype = null;
  mockSetErrorMessage = vi.fn(); 
  vi.clearAllMocks();
  
  Object.assign(defaultMocks.chat, {
    message: '',
    setMessage: vi.fn(),
    sentMessages: [],
    handleSend: vi.fn(),
    errorMessage: '',
    setErrorMessage: mockSetErrorMessage, 
  });
  
  Object.assign(defaultMocks.conversation, {
    messages: [],
    loadingMessages: false,
    activeConversationId: 'test-id'
  });

  mockSessionStorage.removeItem.mockClear();
});

afterEach(() => {
  vi.restoreAllMocks();
});


describe('ChatScreen - Prototype Handling', () => {
  it('should set prototype state when onLoadPrototype is called', async () => {
    const mockSetPrototype = vi.fn();
    const mockSetPrototypeFiles = vi.fn();

    const mockFileTree: FileTree = {
      'index.html': {
        file: {
          contents: '<html><body>Hello world</body></html>'
        }
      },
      'styles.css': {
        file: {
          contents: 'body { color: red; }'
        }
      },
      'app.js': {
        file: {
          contents: 'console.log("Hello");'
        }
      },
      'src': {
        directory: {
          'utils.js': {
            file: {
              contents: 'export const add = (a, b) => a + b;'
            }
          }
        }
      }
    };

    await act(async () => {
      render(
        <MemoryRouter>
          <AuthProvider>
            <ConversationProvider>
              <ChatScreen
                showPrototype={false}
                setPrototype={mockSetPrototype}
                setPrototypeFiles={mockSetPrototypeFiles}
                initialMessage={null}
              />
            </ConversationProvider>
          </AuthProvider>
        </MemoryRouter>
      );
    });

    expect(screen.getByTestId('message-box')).toBeInTheDocument();

    expect(capturedOnLoadPrototype).toBeDefined();
    
    if (capturedOnLoadPrototype) {
      capturedOnLoadPrototype(mockFileTree);

      expect(mockSetPrototype).toHaveBeenCalledWith(true);

      expect(mockSetPrototypeFiles).toHaveBeenCalledWith(mockFileTree);
    }
  });
});

describe('ChatScreen - Initial Message Handling', () => {
  it('should set message, send it after delay, and clear sessionStorage', async () => {
    const mockSetMessage = vi.fn();
    const mockHandleSend = vi.fn();
    const mockInitialMessage = 'Hello, this is a test!';

    defaultMocks.chat.setMessage = mockSetMessage;
    defaultMocks.chat.handleSend = mockHandleSend;

    vi.useFakeTimers();

    render(
      <MemoryRouter>
        <AuthProvider>
          <ConversationProvider>
            <ChatScreen
              showPrototype={false}
              setPrototype={vi.fn()}
              setPrototypeFiles={vi.fn()}
              initialMessage={mockInitialMessage}
            />
          </ConversationProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(mockSetMessage).toHaveBeenCalledWith(mockInitialMessage);
    mockSetMessage.mockClear();

    await act(async () => {
      vi.advanceTimersByTime(500);
    });

    expect(mockHandleSend).toHaveBeenCalledWith(mockInitialMessage);

    expect(mockSessionStorage.removeItem).toHaveBeenCalledWith('initialMessage');
    vi.useRealTimers();
  });
});

describe('ChatScreen - Message Handling', () => {
  it('should deduplicate and sort messages correctly', () => {
    capturedMessages = [];
    
    const createTimestamp = (secondsAgo: number) => {
      const date = new Date();
      date.setSeconds(date.getSeconds() - secondsAgo);
      return date.toISOString();
    };
  
    const mockMessages = [
      { 
        id: '1', 
        role: 'User', 
        content: 'Hello', 
        timestamp: createTimestamp(10) 
      },
      { 
        id: '2', 
        role: 'LLM', 
        content: 'Hi there', 
        timestamp: createTimestamp(8) 
      },
      { 
        id: '3', 
        role: 'LLM', 
        content: 'Hi there', 
        timestamp: createTimestamp(7.5) 
      },
      { 
        id: '4', 
        role: 'User', 
        content: 'How are you?', 
        timestamp: createTimestamp(5) 
      },
    ];

    defaultMocks.conversation.messages = mockMessages.slice(0, 3); 
    defaultMocks.chat.sentMessages = [mockMessages[3]]; 
    
    render(
      <MemoryRouter>
        <AuthProvider>
          <ConversationProvider>
            <ChatScreen
              showPrototype={false}
              setPrototype={vi.fn()}
              setPrototypeFiles={vi.fn()}
              initialMessage={null}
            />
          </ConversationProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    expect(capturedMessages).toHaveLength(3);

    expect(capturedMessages[0].content).toBe('Hello');
    expect(capturedMessages[1].content).toBe('Hi there'); 
    expect(capturedMessages[2].content).toBe('How are you?');
    
    const timestamps = capturedMessages.map(msg => new Date(msg.timestamp).getTime());
    expect(timestamps[0]).toBeLessThan(timestamps[1]);
    expect(timestamps[1]).toBeLessThan(timestamps[2]);
    
    const messageIds = capturedMessages.map(msg => msg.id);
    expect(messageIds).toContain('1');
    expect(messageIds).toContain('2');
    expect(messageIds).not.toContain('3'); 
    expect(messageIds).toContain('4');

  });
});

describe('ChatScreen - Error Handling', () => {
  it('should display toast when errorMessage is set', () => {
    const toastErrorSpy = vi.spyOn(toast, 'error').mockImplementation(() => 'mock-toast-id');
    
    defaultMocks.chat.errorMessage = 'Test error message';
    
    render(
      <MemoryRouter>
        <AuthProvider>
          <ConversationProvider>
            <ChatScreen
              showPrototype={false}
              setPrototype={vi.fn()}
              setPrototypeFiles={vi.fn()}
              initialMessage={null}
            />
          </ConversationProvider>
        </AuthProvider>
      </MemoryRouter>
    );
    
    expect(toastErrorSpy).toHaveBeenCalledWith(
      'Test error message',
      expect.objectContaining({
        onDismiss: expect.any(Function),
        onAutoClose: expect.any(Function),
        closeButton: true,
      })
    );
    
    const [, options] = toastErrorSpy.mock.calls[0];
    
    if (options?.onDismiss) {
      options.onDismiss({ id: 'mock-toast-id' });
      expect(mockSetErrorMessage).toHaveBeenCalledWith('');
    }
    
    mockSetErrorMessage.mockClear();
    if (options?.onAutoClose) {
      options.onAutoClose({ id: 'mock-toast-id' });
      expect(mockSetErrorMessage).toHaveBeenCalledWith('');
    }
    
    toastErrorSpy.mockRestore();
  });
})

describe('ChatScreen - Loading State', () => {
  it('should display loading indicator when loadingMessages is true', () => {
    defaultMocks.conversation.loadingMessages = true;
    
    render(
      <MemoryRouter>
        <AuthProvider>
          <ConversationProvider>
            <ChatScreen
              showPrototype={false}
              setPrototype={vi.fn()}
              setPrototypeFiles={vi.fn()}
              initialMessage={null}
            />
          </ConversationProvider>
        </AuthProvider>
      </MemoryRouter>
    );
    
    expect(screen.getByText('Loading messages...')).toBeInTheDocument();
    
    const loadingOverlay = screen.getByText('Loading messages...').parentElement;
    expect(loadingOverlay).toHaveClass('absolute');
    expect(loadingOverlay).toHaveClass('inset-0');
    expect(loadingOverlay).toHaveClass('flex');
    expect(loadingOverlay).toHaveClass('items-center');
    expect(loadingOverlay).toHaveClass('justify-center');
    expect(loadingOverlay).toHaveClass('bg-background/80');
    expect(loadingOverlay).toHaveClass('z-10');
  });

  it('should not display loading indicator when loadingMessages is false', () => {
    defaultMocks.conversation.loadingMessages = false;
    
    render(
      <MemoryRouter>
        <AuthProvider>
          <ConversationProvider>
            <ChatScreen
              showPrototype={false}
              setPrototype={vi.fn()}
              setPrototypeFiles={vi.fn()}
              initialMessage={null}
            />
          </ConversationProvider>
        </AuthProvider>
      </MemoryRouter>
    );
  
    expect(screen.queryByText('Loading messages...')).not.toBeInTheDocument();
  });
});