import { render, waitFor, screen, fireEvent } from '@testing-library/react';
import { describe, test, expect, beforeEach } from 'vitest';
import { MessageBox } from '../../../components/chat/MessagesBox.js';
import '../../mocks/message-box.mock.jsx';
import { getPrototypeForMessage } from '@/api/FrontEndAPI';

vi.mock('@/api/FrontEndAPI', () => ({
  getPrototypeForMessage: vi.fn().mockResolvedValue([]),
}));

describe('MessageBox Component', () => {
  let onLoadPrototype;

  beforeEach(() => {
    onLoadPrototype = vi.fn();
    vi.clearAllMocks();
  });

  const mockMessages = [
    {
      role: 'User',
      content: 'user test message',
      timestamp: '2023-10-01T10:00:00',
    },
    {
      role: 'Bot',
      content: 'bot test message',
      timestamp: '2023-10-01T10:01:00',
    },
    {
      role: 'LLM',
      content: 'llm test message',
      timestamp: '2023-10-01T10:02:00',
      conversationId: '1234',
      id: '5678',
    },
  ];

  test('scrolls to the most recent message', () => {
    const scrollIntoViewMock = vi.fn();
    window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock;

    render(<MessageBox sentMessages={mockMessages} />);

    expect(scrollIntoViewMock).toHaveBeenCalled();
  });

  test('renders user message correctly', () => {
    render(<MessageBox sentMessages={mockMessages} />);

    const userMessage = screen.getByText('user test message');
    expect(userMessage).toBeInTheDocument();

    const bubble = userMessage.closest('.self-end');
    expect(bubble).toBeInTheDocument();

    const userTimestamp = screen.getByText('10:00 am', { exact: false });
    expect(userTimestamp).toBeInTheDocument();
  });

  test('renders bot message correctly', () => {
    render(<MessageBox sentMessages={mockMessages} />);

    const botMessage = screen.getByText('bot test message');
    expect(botMessage).toBeInTheDocument();

    const botMessageContainer = botMessage.closest('.self-start');
    expect(botMessageContainer).toBeInTheDocument();

    const botTimestamp = screen.getByText('10:01 am', { exact: false });
    expect(botTimestamp).toBeInTheDocument();
  });

  test('does not call API if message role is not "LLM"', async () => {
    const msg = {
      role: 'User',
      content: 'user message',
      conversationId: '123',
      id: '456',
    };

    render(
      <MessageBox sentMessages={[msg]} onLoadPrototype={onLoadPrototype} />
    );

    const userMessageElement = screen.getByText((content, element) => {
      return content.includes('user message');
    });

    expect(userMessageElement).toBeInTheDocument();

    fireEvent.click(userMessageElement);

    expect(getPrototypeForMessage).not.toHaveBeenCalled();
    expect(onLoadPrototype).not.toHaveBeenCalled();
  });

  test('calls API and onLoadPrototype when an LLM message is clicked', async () => {
    const mockPrototypeFiles = { some: 'data' };
    getPrototypeForMessage.mockResolvedValue(mockPrototypeFiles);

    render(
      <MessageBox
        sentMessages={mockMessages}
        onLoadPrototype={onLoadPrototype}
      />
    );

    const llmMessageElement = screen
      .getByText((content, element) => content.includes('llm test message'))
      .closest('.group');

    expect(llmMessageElement).toBeInTheDocument();

    fireEvent.click(llmMessageElement);

    expect(getPrototypeForMessage).toHaveBeenCalledWith('1234', '5678');

    await waitFor(() =>
      expect(onLoadPrototype).toHaveBeenCalledWith(mockPrototypeFiles)
    );
  });

  test('does not call API if coversationId is missing', async () => {
    const msg = { role: 'LLM', content: 'llm message', id: '456' };

    render(
      <MessageBox sentMessages={[msg]} onLoadPrototype={onLoadPrototype} />
    );

    const llmMessageElement = screen.getByText((content, element) => {
      return content.includes('llm message');
    });

    fireEvent.click(screen.getByText('llm message'));

    expect(getPrototypeForMessage).not.toHaveBeenCalled();
    expect(onLoadPrototype).not.toHaveBeenCalled();
  });

  test('handles API errors gracefully', async () => {
    getPrototypeForMessage.mockRejectedValue(new Error('API error'));

    const consoleErrorMock = vi
      .spyOn(console, 'error')
      .mockImplementation(() => {});

    render(
      <MessageBox
        sentMessages={mockMessages}
        onLoadPrototype={onLoadPrototype}
      />
    );

    const llmMessageElement = screen
      .getByText((content) => content.includes('llm test message'))
      .closest('.group');

    expect(llmMessageElement).toBeInTheDocument();

    fireEvent.click(llmMessageElement);

    await waitFor(() => {
      expect(consoleErrorMock).toHaveBeenCalledWith(
        'Error loading prototype:',
        expect.any(Error)
      );
    });

    expect(onLoadPrototype).not.toHaveBeenCalled();

    consoleErrorMock.mockRestore();
  });

  test('calls handleMessageClick when an LLM message is clicked', async () => {
    const handleMessageClick = vi.fn();
    render(
      <MessageBox
        sentMessages={mockMessages}
        onLoadPrototype={() => {}}
        onMessageClick={handleMessageClick}
      />
    );

    const llmMessageElement = screen
      .getByText('llm test message')
      .closest('.group');
    fireEvent.click(llmMessageElement);

    await waitFor(() => expect(handleMessageClick).toHaveBeenCalledTimes(1));
    expect(handleMessageClick).toHaveBeenCalledWith(mockMessages[2]);
  });

  const mockMessagesWithCode = [
    {
      role: 'Bot',
      content: 'Here is some `inline code` and\n```\ncode block\n```',
      timestamp: '2023-10-01T10:01:00',
    },
  ];

  test('renders inline code correctly', () => {
    render(<MessageBox sentMessages={mockMessagesWithCode} />);
    const inlineCode = screen.getByText('inline code');
    expect(inlineCode).toBeInTheDocument();
    expect(inlineCode.tagName).toBe('CODE');
  });

  test('renders code block within pre tag', () => {
    render(
      <MessageBox
        sentMessages={[
          {
            role: 'Bot',
            content: '```\nblock code\n```',
            timestamp: '2023-10-01T10:01:00',
          },
        ]}
      />
    );

    const codeBlockElement = screen.getByText((content, element) => {
      return element.tagName === 'CODE' && content.includes('block code');
    });

    expect(codeBlockElement).toBeInTheDocument();
    expect(codeBlockElement.closest('pre')).toHaveClass('whitespace-pre-wrap');
  });

  test('renders message with correct structure', () => {
    render(<MessageBox sentMessages={mockMessagesWithCode} />);

    const botMessageContainer = screen
      .getByText(/Here is some/)
      .closest('.self-start');
    expect(botMessageContainer).toBeInTheDocument();
  });

  test('renders timestamp correctly', () => {
    render(<MessageBox sentMessages={mockMessagesWithCode} />);

    const timestamp = screen.getByText('10:01 am', { exact: false });
    expect(timestamp).toBeInTheDocument();
  });

  const mockMessagesWithEmptyContent = [
    {
      role: 'User',
      content: '',
      timestamp: '2023-10-01T10:00:00',
    },
    {
      role: 'Bot',
      content: undefined,
      timestamp: '2023-10-01T10:01:00',
    },
    {
      role: 'User',
      content: null,
      timestamp: '2023-10-01T10:02:00',
    },
  ];

  test('handles messages with empty content gracefully', () => {
    render(<MessageBox sentMessages={mockMessagesWithEmptyContent} />);

    ['10:00 am', '10:01 am', '10:02 am'].forEach((time) => {
      expect(screen.getByText(time, { exact: false })).toBeInTheDocument();
    });
  });

  test('handles messages with null content gracefully', () => {
    const mockMessagesWithNullContent = [
      {
        role: 'User',
        content: null,
        timestamp: '2023-10-01T10:00:00',
      },
    ];

    render(<MessageBox sentMessages={mockMessagesWithNullContent} />);

    mockMessagesWithEmptyContent.forEach((msg) => {
      const time = new Date(msg.timestamp).toLocaleTimeString('en-GB', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      });

      const timestamp = screen.getByText('10:00 am', { exact: false });
      expect(timestamp).toBeInTheDocument();
    });
  });
});
