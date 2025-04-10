import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import ChatScreen from '../../components/chat/ChatScreen.tsx';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router-dom';
import { vi, test, expect, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';

import { AuthProvider } from '@/contexts/AuthContext';
import { ConversationProvider } from '@/contexts/ConversationContext';

globalThis.fetch = vi.fn();
const mockSetPrototype = vi.fn();
const mockSetPrototypeId = vi.fn();
const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

beforeEach(() => {
  vi.resetAllMocks();
});

test('Renders chat page', () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <ChatScreen
            setPrototype={mockSetPrototype}
            setPrototypeId={mockSetPrototypeId}
          />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );
  const element = screen.getByPlaceholderText(/How can we help you today?/i);
  expect(element).toBeInTheDocument();
});

test('Enter text in chat', async () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <ChatScreen
            setPrototype={mockSetPrototype}
            setPrototypeId={mockSetPrototypeId}
          />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
  await userEvent.type(userchat, 'Hello!');
  expect(userchat).toHaveValue('Hello!');
});

test('Press enter button', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    text: vi.fn().mockResolvedValue('Mock LLM response'),
  });
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <ChatScreen
            setPrototype={mockSetPrototype}
            setPrototypeId={mockSetPrototypeId}
          />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
  await userEvent.type(userchat, 'Hello!');
  await userEvent.keyboard('{Enter}');
  await waitFor(
    () => {
      expect(userchat).toHaveValue('');
    },
    { timeout: 3000 }
  );
});

test('Valid post request', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    text: vi.fn().mockResolvedValue('Mock LLM response'),
  });
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <ChatScreen
            setPrototype={mockSetPrototype}
            setPrototypeId={mockSetPrototypeId}
          />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
  await userEvent.type(userchat, 'Hello!');
  await userEvent.keyboard('{Enter}');
  await waitFor(
    () => {
      expect(userchat).toHaveValue('');
    },
    { timeout: 3000 }
  );

  expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/chat/json', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: expect.any(String),
  });
});

test('Clicking send button sends a message', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    text: vi.fn().mockResolvedValue('Mock LLM response'),
  });
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <ChatScreen
            setPrototype={mockSetPrototype}
            setPrototypeId={mockSetPrototypeId}
          />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
  await userEvent.type(userchat, ' ');
  const sendButton = screen.getByText('Send');
  fireEvent.click(sendButton);
  await waitFor(
    () => {
      expect(userchat).toHaveValue(' ');
    },
    { timeout: 3000 }
  );

  await userEvent.type(userchat, 'Hello!');
  fireEvent.click(sendButton);
  await waitFor(
    () => {
      expect(userchat).toHaveValue('');
    },
    { timeout: 3000 }
  );

  expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/chat/json', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: expect.any(String),
  });

  await userEvent.type(userchat, 'This is an inline `code` example.');
  await userEvent.keyboard('{Enter}');

  await userEvent.type(userchat, '```\nThis is a block code example\n```');
  await userEvent.keyboard('{Enter}');
});

test('Initial message set when page loads', async () => {
  vi.doMock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
      ...actual,
      useLocation: vi.fn().mockReturnValue({
        pathname: '/generate',
        state: { initialMessage: 'Test initial message' },
      }),
    };
  });

  const ChatScreen = (await import('../../components/chat/ChatScreen.tsx'))
    .default;
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <ChatScreen
            setPrototype={mockSetPrototype}
            setPrototypeId={mockSetPrototypeId}
          />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
  await waitFor(
    () => {
      expect(userchat).toHaveValue('');
    },
    { timeout: 3000 }
  );
});
