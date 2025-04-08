import { render, screen, fireEvent, act } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import { AppSidebar } from '@/components/sidebar/AppSidebar';
import { useConversation } from '@/contexts/ConversationContext';
import { toast } from 'sonner';

vi.mock('@/contexts/ConversationContext', () => ({
  useConversation: vi.fn(),
}));

vi.mock('@/components/sidebar/NavMain', () => ({
  // eslint-disable-next-line react/prop-types
  NavMain: ({ items }) => (
    <div data-testid="nav-main">
      {/* eslint-disable-next-line react/prop-types */}
      {items[0].items.map((item) => (
        <div key={item.id}>
          <button
            data-testid={`conversation-${item.id}`}
            data-active={item.isActive.toString()}
            onClick={item.onClick}
          >
            {item.title}
          </button>
          {item.actions && item.actions.map((action, index) => (
            <button
              key={index}
              data-testid={`action-${item.id}-${index}`}
              onClick={(e) => action.onClick(e)}
            >
              Action
            </button>
          ))}
        </div>
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

vi.mock('@/components/ui/Dialog', () => ({
  Dialog: ({ children, open }) => (
    <div data-testid="dialog" data-open={open || false}>
      {children}
    </div>
  ),
  DialogContent: ({ children }) => (
    <div data-testid="dialog-content">{children}</div>
  ),
  DialogHeader: ({ children }) => (
    <div data-testid="dialog-header">{children}</div>
  ),
  DialogTitle: ({ children }) => (
    <div data-testid="dialog-title">{children}</div>
  ),
  DialogDescription: ({ children }) => (
    <div data-testid="dialog-description">{children}</div>
  ),
  DialogFooter: ({ children }) => (
    <div data-testid="dialog-footer">{children}</div>
  ),
}));

vi.mock('@/components/ui/Button', () => ({
  Button: ({ children, onClick, variant }) => (
    <button
      data-testid={`button-${variant || 'default'}`}
      onClick={onClick}
    >
      {children}
    </button>
  ),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  }
}));

vi.mock('lucide-react', () => ({
  History: () => <div data-testid="history-icon">History Icon</div>,
  PlusCircle: () => <div data-testid="plus-circle-icon">Plus Circle Icon</div>,
  Trash2: () => <div data-testid="trash-icon">Trash Icon</div>
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

test('AppSidebar includes delete functionality in conversation items', () => {
  const mockDeleteConversation = vi.fn();
  useConversation.mockReturnValue({
    conversations: [
      { id: '1', name: 'Conversation 1' },
    ],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
    deleteConversation: mockDeleteConversation
  });

  render(<AppSidebar />);
  
  expect(screen.getByTestId('action-1-0')).toBeInTheDocument();
});

test('Delete dialog opens when initiated and can be cancelled', () => {
  const mockDeleteConversation = vi.fn();
  useConversation.mockReturnValue({
    conversations: [
      { id: '1', name: 'Conversation 1' },
    ],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
    deleteConversation: mockDeleteConversation
  });

  render(<AppSidebar />);
  
  const initialDialog = screen.getByTestId('dialog');
  expect(initialDialog.getAttribute('data-open')).toBe('false');
  
  const actionButton = screen.getByTestId('action-1-0');
  fireEvent.click(actionButton);
  
  const openDialog = screen.getByTestId('dialog');
  expect(openDialog.getAttribute('data-open')).toBe('true');
  
  expect(screen.getByTestId('dialog-title')).toHaveTextContent('Delete Conversation');
  expect(screen.getByTestId('dialog-description')).toHaveTextContent('Are you sure');
  
  const cancelButton = screen.getByTestId('button-outline');
  expect(cancelButton).toHaveTextContent('Cancel');
  fireEvent.click(cancelButton);
  
  expect(mockDeleteConversation).not.toHaveBeenCalled();
});

test('Confirming deletion calls deleteConversation with correct ID', async () => {
  const mockDeleteConversation = vi.fn().mockResolvedValue(true);
  useConversation.mockReturnValue({
    conversations: [
      { id: '1', name: 'Conversation 1' },
    ],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
    deleteConversation: mockDeleteConversation
  });

  render(<AppSidebar />);
  
  const actionButton = screen.getByTestId('action-1-0');
  fireEvent.click(actionButton);
  
  const deleteButton = screen.getByTestId('button-destructive');
  expect(deleteButton).toHaveTextContent('Delete');
  
  await act(async () => {
    await fireEvent.click(deleteButton);
  });
  
  expect(mockDeleteConversation).toHaveBeenCalledWith('1');
  expect(toast.success).toHaveBeenCalledWith('Conversation deleted');
});

test('Failed deletion shows error toast', async () => {
  const mockDeleteConversation = vi.fn().mockResolvedValue(false);
  useConversation.mockReturnValue({
    conversations: [
      { id: '1', name: 'Conversation 1' },
    ],
    activeConversationId: '1',
    setActiveConversationId: vi.fn(),
    createConversation: vi.fn(),
    deleteConversation: mockDeleteConversation
  });

  render(<AppSidebar />);
  
  const actionButton = screen.getByTestId('action-1-0');
  fireEvent.click(actionButton);
  
  const deleteButton = screen.getByTestId('button-destructive');
  
  await act(async () => {
    await fireEvent.click(deleteButton);
  });
  
  expect(toast.error).toHaveBeenCalledWith('Failed to delete conversation');
});
