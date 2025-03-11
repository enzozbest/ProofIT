import react from 'react';
import { render, screen } from '@testing-library/react';
import { describe, test, expect } from 'vitest';
import { MessageBox } from '../../components/messages-box.jsx';
import '../mocks/message-box.mock.jsx';

describe('MessageBox Component', () => {
  const mockMessages = [
    {
      role: 'User',
      content: 'user test message',
      timestamp: '2023-10-01, 10:00 AM',
    },
    {
      role: 'Bot',
      content: 'bot test message',
      timestamp: '2023-10-01, 10:01 AM',
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

    const userMessageContainer = userMessage.parentElement;
    expect(userMessageContainer).toHaveStyle('align-self: flex-end');
    expect(userMessageContainer).toHaveStyle(
      'background-color: rgb(241, 241, 241)'
    );
    expect(userMessageContainer).toHaveStyle('border-radius: 5px');
    expect(userMessageContainer).toHaveStyle('max-width: 70%');

    const userTimestamp = screen.getByText((content, element) => {
      return element.textContent.trim() === '10:00 AM';
    });
    expect(userTimestamp).toBeInTheDocument();
  });

  test('renders bot message correctly', () => {
    render(<MessageBox sentMessages={mockMessages} />);

    const botMessage = screen.getByText('bot test message');
    expect(botMessage).toBeInTheDocument();

    const botMessageContainer = botMessage.parentElement;
    expect(botMessageContainer).toHaveStyle('align-self: flex-start');
    expect(botMessageContainer).toHaveStyle(
      'background-color: rgb(241, 241, 241)'
    );
    expect(botMessageContainer).toHaveStyle('border-radius: 5px');
    expect(botMessageContainer).toHaveStyle('max-width: 70%');

    const botTimestamp = screen.getByText((content, element) => {
      return element.textContent.trim() === '10:01 AM';
    });
    expect(botTimestamp).toBeInTheDocument();
  });

  const mockMessagesWithCode = [
    {
      role: 'Bot',
      content: 'Here is some `inline code` and\n```\ncode block\n```',
      timestamp: '2023-10-01, 10:01 AM',
    },
  ];

  test('renders inline code correctly', () => {
    const { container } = render(
      <MessageBox sentMessages={mockMessagesWithCode} />
    );
    const inlineCodeElements = container.querySelectorAll('code');
    expect(inlineCodeElements.length).toBeGreaterThan(0);
  });

  test('renders code block within pre tag', () => {
    const { container } = render(
      <MessageBox sentMessages={mockMessagesWithCode} />
    );

    const preElements = container.querySelectorAll(
      'pre.whitespace-pre-wrap.pt-2'
    );
    expect(preElements.length).toBeGreaterThan(0);
  });

  test('renders message with correct structure', () => {
    const { container } = render(
      <MessageBox sentMessages={mockMessagesWithCode} />
    );

    const messageContainer = container.querySelector(
      'div[style*="align-self: flex-start"]'
    );
    expect(messageContainer).toBeInTheDocument();

    const markdownContent = messageContainer?.querySelector('div > *'); // First child of the message container
    expect(markdownContent).toBeInTheDocument();
  });

  test('renders timestamp correctly', () => {
    render(<MessageBox sentMessages={mockMessagesWithCode} />);

    const timestamp = screen.getByText((content, element) => {
      return element.textContent.trim() === '10:01 AM';
    });
    expect(timestamp).toBeInTheDocument();
  });

  const mockMessagesWithEmptyContent = [
    {
      role: 'User',
      content: '',
      timestamp: '2023-10-01, 10:00 AM',
    },
    {
      role: 'Bot',
      content: undefined,
      timestamp: '2023-10-01, 10:01 AM',
    },
    {
      role: 'User',
      content: null,
      timestamp: '2023-10-01, 10:02 AM',
    },
  ];

  test('handles messages with empty content gracefully', () => {
    const { container, debug } = render(
      <MessageBox sentMessages={mockMessagesWithEmptyContent} />
    );

    const timestamp1 = screen.getByText('10:00 AM');
    const timestamp2 = screen.getByText('10:01 AM');
    const timestamp3 = screen.getByText('10:02 AM');
    expect(timestamp1).toBeInTheDocument();
    expect(timestamp2).toBeInTheDocument();
    expect(timestamp3).toBeInTheDocument();

    [timestamp1, timestamp2, timestamp3].forEach((timestamp) => {
      const messageContainer = timestamp.parentElement;
      expect(messageContainer).toHaveStyle('padding: 10px');
      expect(messageContainer).toHaveStyle(
        'background-color: rgb(241, 241, 241)'
      );
      expect(messageContainer).toHaveStyle('border-radius: 5px');
      expect(messageContainer).toHaveStyle('max-width: 70%');
    });
  });

  test('handles messages with null content gracefully', () => {
    const mockMessagesWithNullContent = [
      {
        role: 'User',
        content: null,
        timestamp: '2023-10-01, 10:00 AM',
      },
    ];

    render(<MessageBox sentMessages={mockMessagesWithNullContent} />);

    const timestamp = screen.getByText('10:00 AM');
    expect(timestamp).toBeInTheDocument();

    const messageContainer = timestamp.parentElement;
    expect(messageContainer).toHaveStyle('padding: 10px');
    expect(messageContainer).toHaveStyle(
      'background-color: rgb(241, 241, 241)'
    );
    expect(messageContainer).toHaveStyle('border-radius: 5px');
    expect(messageContainer).toHaveStyle('max-width: 70%');
  });
});
