import react from 'react';
import { render, screen } from '@testing-library/react';
import { describe, test, expect } from 'vitest';
import { MessageBox } from '../../components/chat/messages-box.jsx';
import '../mocks/message-box.mock.jsx';

describe('MessageBox Component', () => {
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
    render(<MessageBox
      sentMessages={[
        {
          role: 'Bot',
          content: '```\nblock code\n```',
          timestamp: '2023-10-01T10:01:00',
        },
      ]}
      />,
    );

    const codeBlockElement = screen.getByText((content, element) => {
      return element.tagName === 'CODE' && content.includes('block code');
    });

    expect(codeBlockElement).toBeInTheDocument();
    expect(codeBlockElement.closest('pre')).toHaveClass('whitespace-pre-wrap');
  });

  test('renders message with correct structure', () => {
    render(<MessageBox sentMessages={mockMessagesWithCode} />);

    const botMessageContainer = screen.getByText(/Here is some/).closest('.self-start');
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
  })})