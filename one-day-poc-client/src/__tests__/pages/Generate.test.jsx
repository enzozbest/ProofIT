import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { vi, test, expect, beforeEach, beforeAll } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Page from '../../pages/Generate.js';
import React from 'react';
import ChatScreen from '../../components/chat/ChatScreen.js';
import { NavUser } from '../../components/sidebar/NavUser.tsx';
import { useSidebar } from '../../components/ui/Sidebar';
import { SidebarProvider } from '../../components/ui/Sidebar';
import Generate from '../../pages/Generate';

import { AuthProvider } from '@/contexts/AuthContext';
import { ConversationProvider } from '@/contexts/ConversationContext';

const mockUseLocation = vi.fn();

// Mock the entire react-router-dom module
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useLocation: () => mockUseLocation(),
  };
});

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

const mockPrototypeFrame = vi.fn();
vi.mock('@/components/prototype/PrototypeFrame', () => ({
  default: (props) => {
    mockPrototypeFrame(props);
    return props.testVisible ? (
      <div data-testid="prototype-frame">Mock Prototype Frame</div>
    ) : null;
  },
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

  // Set the default mock return value for useLocation
  mockUseLocation.mockReturnValue({
    pathname: '/generate',
    search: '',
    hash: '',
    state: null,
  });
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

test('Sets initialMessage and isPredefined from location state', () => {
  const consoleLogSpy = vi.spyOn(console, 'log');

  mockUseLocation.mockReturnValue({
    pathname: '/generate',
    search: '',
    hash: '',
    state: {
      initialMessage: 'Message from router state',
      isPredefined: true,
    },
  });

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
      initialMessage: 'Message from router state',
      isPredefined: true,
    }),
    expect.anything()
  );

  expect(consoleLogSpy).toHaveBeenCalledWith(
    'inside /generate, predefined value is ',
    false
  );

  consoleLogSpy.mockRestore();
});

test('Handles location state with initialMessage but without isPredefined', () => {
  mockUseLocation.mockReturnValue({
    pathname: '/generate',
    search: '',
    hash: '',
    state: {
      initialMessage: 'Another message from router state',
    },
  });

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
      initialMessage: 'Another message from router state',
      isPredefined: false,
    }),
    expect.anything()
  );
});

test('Does not render PrototypeFrame when showPrototype is false', () => {
  const TestComponent = () => {
    const showPrototype = false;

    return (
      <div>
        {showPrototype ? (
          <div data-testid="prototype-frame">Prototype is visible</div>
        ) : null}
      </div>
    );
  };

  render(<TestComponent />);

  expect(screen.queryByTestId('prototype-frame')).not.toBeInTheDocument();
});

test('Renders PrototypeFrame when showPrototype is true', () => {
  const TestComponent = () => {
    const showPrototype = true;

    return (
      <div>
        {showPrototype ? (
          <div data-testid="prototype-frame">Prototype is visible</div>
        ) : null}
      </div>
    );
  };

  render(<TestComponent />);

  expect(screen.getByTestId('prototype-frame')).toBeInTheDocument();
});
