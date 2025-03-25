import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { vi, test, expect, beforeEach, beforeAll } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Page from '../../pages/Generate.js';
import userEvent from '@testing-library/user-event';
import React from 'react';
import ChatScreen from '../../components/chat/ChatScreen.js';
import { NavUser } from '../../components/sidebar/NavUser.tsx';
import { useSidebar } from '../../components/ui/Sidebar';
import { SidebarProvider } from '../../components/ui/Sidebar';
import Generate from '../../pages/Generate';

import { AuthProvider } from '@/contexts/AuthContext';
import { ConversationProvider } from '@/contexts/ConversationContext';

vi.mock('@/components/ui/Sidebar', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useSidebar: vi.fn().mockReturnValue({ isMobile: false }),
  };
});

vi.mock('../../components/chat/ChatScreen', () => ({
  default: vi
    .fn()
    .mockImplementation((props) => (
      <div data-testid="mocked-chat-screen">
        Mocked ChatScreen with initialMessage: {props.initialMessage}
      </div>
    )),
}));

beforeAll(() => {
  globalThis.window.matchMedia = vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));

  vi.clearAllMocks();
});

beforeEach(() => {
  vi.clearAllMocks();
  sessionStorage.clear();
  ChatScreen.mockClear();
});

test('Sets initialMessage when found in sessionStorage', async () => {
  sessionStorage.setItem('initialMessage', 'Hello World');

  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <Page />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  expect(ChatScreen).toHaveBeenCalledWith(
    expect.objectContaining({
      initialMessage: 'Hello World',
    }),
    expect.anything()
  );

  expect(
    screen.getByText('Mocked ChatScreen with initialMessage: Hello World')
  ).toBeInTheDocument();
});

test('Does not render PrototypeFrame when showPrototype is false', () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <Generate showPrototype={false} prototypeFiles={[]} />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  expect(screen.queryByTestId('prototype-frame')).not.toBeInTheDocument();
  expect(screen.queryByTestId('prototype-frame')).toBeNull();
});

test('Renders generate page', () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <Page />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  const mockedChatScreen = screen.getByTestId('mocked-chat-screen');
  expect(mockedChatScreen).toBeInTheDocument();
});

test('Chat screen toggles', () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <Page />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );
  const toggleButton = screen.getByTestId('toggle-button');
  const chatScreenDiv = document.querySelector('.w-\\[450px\\]');

  expect(chatScreenDiv).toHaveClass('max-w-[450px]');

  fireEvent.click(toggleButton);
  expect(chatScreenDiv).toHaveClass('max-w-0');
});

test('Prototype frame displays', async () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <Page />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  await waitFor(() => {
    const prototypeContainer = document.querySelector('.flex-1.h-full');
    expect(prototypeContainer).not.toBeNull();
  });
});

test('Mobile sidebar renders correctly', async () => {
  useSidebar.mockReturnValue({ isMobile: true });

  render(
    <MemoryRouter>
      <SidebarProvider>
        <NavUser
          user={{ name: 'Test User', email: 'test@example.com', avatar: '' }}
        />
      </SidebarProvider>
    </MemoryRouter>
  );

  const button = screen.getByRole('button');
  expect(button).toBeInTheDocument();
});

test('Non-mobile sidebar renders correctly', async () => {
  useSidebar.mockReturnValue({ isMobile: false });
  render(
    <MemoryRouter>
      <SidebarProvider>
        <NavUser
          user={{ name: 'Test User', email: 'test@example.com', avatar: '' }}
        />
      </SidebarProvider>
    </MemoryRouter>
  );

  const button = screen.getByRole('button');
  expect(button).toBeInTheDocument();
});

test('Renders background image correctly', () => {
  render(
    <MemoryRouter>
      <AuthProvider>
        <ConversationProvider>
          <Generate />
        </ConversationProvider>
      </AuthProvider>
    </MemoryRouter>
  );

  const containerDiv = screen.getByTestId('container').closest('div');
  expect(containerDiv).toHaveStyle({
    backgroundImage: "url('/background.svg')",
  });
});
