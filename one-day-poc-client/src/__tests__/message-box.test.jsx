import react from 'react';
import { render, screen } from '@testing-library/react';
import { describe, test, expect } from 'vitest';
import { MessageBox } from '../components/messages-box';
import './message-box.mock';

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

    // Check if scrollIntoView was called
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

    // Find pre elements with the correct classes
    const preElements = container.querySelectorAll(
      'pre.whitespace-pre-wrap.pt-2'
    );
    expect(preElements.length).toBeGreaterThan(0);
  });

  test('renders message with correct structure', () => {
    const { container } = render(
      <MessageBox sentMessages={mockMessagesWithCode} />
    );

    // Check for the main message container
    const messageContainer = container.querySelector(
      'div[style*="align-self: flex-start"]'
    );
    expect(messageContainer).toBeInTheDocument();

    // Check for Markdown component rendering
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
});
