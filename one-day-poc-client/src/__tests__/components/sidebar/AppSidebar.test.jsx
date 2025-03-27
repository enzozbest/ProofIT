import { render, screen, fireEvent } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import { AppSidebar } from '@/components/sidebar/AppSidebar';
import { useConversation } from '@/contexts/ConversationContext';

vi.mock('@/contexts/ConversationContext', () => ({
  useConversation: vi.fn(),
}));

vi.mock('@/components/sidebar/NavMain', () => ({
  // eslint-disable-next-line react/prop-types
  NavMain: ({ items }) => (
    <div data-testid="nav-main">
      {/* eslint-disable-next-line react/prop-types */}
      {items[0].items.map((item) => (
        <button
          key={item.id}
          data-testid={`conversation-${item.id}`}
          data-active={item.isActive.toString()}
          onClick={item.onClick}
        >
          {item.title}
        </button>
      ))}
    </div>
  ),
}));

vi.mock('@/components/sidebar/NavUser', () => ({
  NavUser: () => <div data-testid="nav-user">Nav User</div>,
}));

vi.mock('@/components/ui/Sidebar', () => ({
  // eslint-disable-next-line react/prop-types
  Sidebar: ({ children, collapsible, className, ...props }) => (
    <div
      data-testid="sidebar"
      data-collapsible={collapsible}
      className={className}
      {...props}
    >
      {children}
    </div>
  ),
  // eslint-disable-next-line react/prop-types
  SidebarHeader: ({ children }) => (
    <div data-testid="sidebar-header">{children}</div>
  ),
  // eslint-disable-next-line react/prop-types
  SidebarContent: ({ children }) => (
    <div data-testid="sidebar-content">{children}</div>
  ),
  // eslint-disable-next-line react/prop-types
  SidebarFooter: ({ children }) => (
    <div data-testid="sidebar-footer">{children}</div>
  ),
  SidebarRail: () => <div data-testid="sidebar-rail"></div>,
  SidebarMenuButton: ({ children, tooltip, className, ...props }) => (
    <button data-tooltip={tooltip} className={className} {...props}>
      {children}
    </button>
  ),
}));

// vi.mock('@/components/ui/Button', () => ({
//   // eslint-disable-next-line react/prop-types
//   Button: ({ children, onClick, variant, className, ...props }) => (
//     <button
//       data-testid="new-chat-button"
//       onClick={onClick}
//       data-variant={variant}
//       className={className}
//       {...props}
//     >
//       {children}
//     </button>
//   ),
// }));

vi.mock('lucide-react', () => ({
  History: () => <div data-testid="history-icon">History Icon</div>,
  PlusCircle: () => <div data-testid="plus-circle-icon">Plus Circle Icon</div>,
}));



beforeEach(() => {
  vi.resetAllMocks();
  vi.resetModules();
});

test('Renders sidebar with conversations', () => {
  useConversation.mockReturnValue({
    conversations: [
      { id: '1', name: 'Conversation 1' },
      { id: '2', name: 'Conversation 2' },
    ],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
  });

  render(<AppSidebar />);

  expect(screen.getByTestId('sidebar')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-header')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-content')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-footer')).toBeInTheDocument();
  expect(screen.getByTestId('nav-user')).toBeInTheDocument();
  expect(screen.getByTestId('new-chat-button')).toBeInTheDocument();
  expect(screen.getByText('New Chat')).toBeInTheDocument();
  expect(screen.getByTestId('nav-main')).toBeInTheDocument();
  expect(screen.getByTestId('conversation-1')).toBeInTheDocument();
  expect(screen.getByTestId('conversation-2')).toBeInTheDocument();
});

test('New Chat button calls createConversation when clicked', () => {
  const mockCreateConversation = vi.fn();
  useConversation.mockReturnValue({
    conversations: [{ id: '1', name: 'Conversation 1' }],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: mockCreateConversation,
  });

  render(<AppSidebar />);

  fireEvent.click(screen.getByTestId('new-chat-button'));

  expect(mockCreateConversation).toHaveBeenCalledTimes(1);
});

test('Clicking a conversation calls setActiveConversationId with the right ID', () => {
  const mockSetActiveConversationId = vi.fn();
  useConversation.mockReturnValue({
    conversations: [
      { id: '1', name: 'Conversation 1' },
      { id: '2', name: 'Conversation 2' },
    ],
    activeConversationId: '1',
    setActiveConversationId: mockSetActiveConversationId,
    createConversation: vi.fn(),
  });

  render(<AppSidebar />);

  fireEvent.click(screen.getByTestId('conversation-2'));

  expect(mockSetActiveConversationId).toHaveBeenCalledWith('2');
});

test('Active conversation is highlighted', () => {
  useConversation.mockReturnValue({
    conversations: [
      { id: '1', name: 'Conversation 1' },
      { id: '2', name: 'Conversation 2' },
    ],
    activeConversationId: '2',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
  });

  render(<AppSidebar />);

  expect(screen.getByTestId('conversation-2').getAttribute('data-active')).toBe(
    'true'
  );
  expect(screen.getByTestId('conversation-1').getAttribute('data-active')).toBe(
    'false'
  );
});

test('Renders with empty conversations array', () => {
  useConversation.mockReturnValue({
    conversations: [],
    activeConversationId: null,
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
  });

  render(<AppSidebar />);

  expect(screen.getByTestId('sidebar')).toBeInTheDocument();
  expect(screen.getByTestId('new-chat-button')).toBeInTheDocument();
  expect(screen.getByTestId('nav-main')).toBeInTheDocument();
  expect(screen.queryByTestId(/conversation-/)).not.toBeInTheDocument();
});

test('Sidebar can be passed additional props', () => {
  useConversation.mockReturnValue({
    conversations: [{ id: '1', name: 'Conversation 1' }],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
  });

  render(<AppSidebar data-custom="test-value" />);

  expect(screen.getByTestId('sidebar').getAttribute('data-custom')).toBe(
    'test-value'
  );
});

test('Sidebar has the correct attributes', () => {
  useConversation.mockReturnValue({
    conversations: [{ id: '1', name: 'Conversation 1' }],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
  });

  render(<AppSidebar />);

  const sidebar = screen.getByTestId('sidebar');
  expect(sidebar.getAttribute('data-collapsible')).toBe('icon');
  expect(sidebar.className).toContain('bg-background/85');
});
