/* eslint-disable react/prop-types */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import SidebarWrapper from '@/components/sidebar/SidebarWrapper';
import { useConversation } from '@/contexts/ConversationContext';

// Mock the hooks and components used by SidebarWrapper
vi.mock('@/contexts/ConversationContext', () => ({
  useConversation: vi.fn(),
}));

vi.mock('@/components/sidebar/AppSidebar', () => ({
  AppSidebar: () => <div data-testid="app-sidebar">App Sidebar</div>,
}));

vi.mock('@/components/ui/Typography', () => ({
  TypographySmall: ({ children }) => (
    <span data-testid="typography-small">{children}</span>
  ),
}));

vi.mock('@/components/ui/Popover', () => ({
  Popover: ({ children }) => <div data-testid="popover">{children}</div>,
  PopoverContent: ({ children }) => <div data-testid="popover-content">{children}</div>,
  PopoverTrigger: ({ children, className }) => (
    <button data-testid="popover-trigger" className={className}>
      {children}
    </button>
  ),
}));

vi.mock('@/components/ui/Sidebar', () => ({
  SidebarInset: ({ children, className }) => (
    <div data-testid="sidebar-inset" className={className}>
      {children}
    </div>
  ),
  SidebarProvider: ({ children }) => <div data-testid="sidebar-provider">{children}</div>,
  SidebarTrigger: ({ className }) => (
    <button data-testid="sidebar-trigger" className={className}></button>
  ),
}));

vi.mock('@/components/ui/Button', () => ({
  Button: ({ children, className, onClick }) => (
    <button data-testid="button" className={className} onClick={onClick}>
      {children}
    </button>
  ),
}));

vi.mock('@/components/ui/Input', () => ({
  Input: ({ id, placeholder, value, onChange }) => (
    <input
      data-testid="input"
      id={id}
      placeholder={placeholder}
      value={value}
      onChange={onChange}
    />
  ),
}));

vi.mock('@/components/ui/Label', () => ({
  Label: ({ children, htmlFor }) => (
    <label data-testid="label" htmlFor={htmlFor}>
      {children}
    </label>
  ),
}));

vi.mock('@radix-ui/react-icons', () => ({
  ChevronDownIcon: () => <div data-testid="chevron-down-icon">ChevronDownIcon</div>,
}));

vi.mock('lucide-react', () => ({
  Share: () => <div data-testid="share-icon">Share</div>,
  Rocket: () => <div data-testid="rocket-icon">Rocket</div>,
}));

beforeEach(() => {
  vi.resetAllMocks();
});

test('Renders sidebar wrapper with default project name', () => {
  useConversation.mockReturnValue({
    conversations: [],
    activeConversationId: null,
    updateConversationName: vi.fn(),
  });

  render(
    <SidebarWrapper>
      <div data-testid="children-content">Children Content</div>
    </SidebarWrapper>
  );

  expect(screen.getByTestId('sidebar-provider')).toBeInTheDocument();
  expect(screen.getByTestId('app-sidebar')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-inset')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-trigger')).toBeInTheDocument();
  expect(screen.getByTestId('typography-small')).toBeInTheDocument();
  expect(screen.getByTestId('typography-small')).toHaveTextContent('Untitled Project');
  expect(screen.getByTestId('children-content')).toBeInTheDocument();
});

test('Updates project name when user renames it', async () => {
  const mockUpdateConversationName = vi.fn();
  useConversation.mockReturnValue({
    conversations: [],
    activeConversationId: 'conversation-1',
    updateConversationName: mockUpdateConversationName,
  });

  render(
    <SidebarWrapper>
      <div>Children Content</div>
    </SidebarWrapper>
  );

  // Open the popover
  fireEvent.click(screen.getByTestId('popover-trigger'));

  // Change the input value
  const input = screen.getByTestId('input');
  fireEvent.change(input, { target: { value: 'New Project Name' } });

  // Click save button
  fireEvent.click(screen.getByTestId('button'));

  // Check if the project name is updated
  expect(screen.getByTestId('typography-small')).toHaveTextContent('New Project Name');

  // Check if updateConversationName was called with the correct arguments
  expect(mockUpdateConversationName).toHaveBeenCalledWith('conversation-1', 'New Project Name');
});

test('Loads active conversation name when available', async () => {
  useConversation.mockReturnValue({
    conversations: [
      { id: 'conversation-1', name: 'Existing Project' }
    ],
    activeConversationId: 'conversation-1',
    updateConversationName: vi.fn(),
  });

  render(
    <SidebarWrapper>
      <div>Children Content</div>
    </SidebarWrapper>
  );

  // Check if the project name is loaded from the active conversation
  expect(screen.getByTestId('typography-small')).toHaveTextContent('Existing Project');
});

test('Handles project rename with no active conversation', async () => {
  const mockUpdateConversationName = vi.fn();
  useConversation.mockReturnValue({
    conversations: [],
    activeConversationId: null,
    updateConversationName: mockUpdateConversationName,
  });

  render(
    <SidebarWrapper>
      <div>Children Content</div>
    </SidebarWrapper>
  );

  // Open the popover
  fireEvent.click(screen.getByTestId('popover-trigger'));

  // Change the input value
  const input = screen.getByTestId('input');
  fireEvent.change(input, { target: { value: 'New Project Name' } });

  // Click save button
  fireEvent.click(screen.getByTestId('button'));

  // Check if the project name is updated
  expect(screen.getByTestId('typography-small')).toHaveTextContent('New Project Name');

  // Function should not be called when there's no active conversation
  expect(mockUpdateConversationName).not.toHaveBeenCalled();
});

test('Children are rendered correctly in the content area', () => {
  useConversation.mockReturnValue({
    conversations: [],
    activeConversationId: null,
    updateConversationName: vi.fn(),
  });

  render(
    <SidebarWrapper>
      <div data-testid="test-child-1">Child 1</div>
      <div data-testid="test-child-2">Child 2</div>
    </SidebarWrapper>
  );

  expect(screen.getByTestId('test-child-1')).toBeInTheDocument();
  expect(screen.getByTestId('test-child-2')).toBeInTheDocument();

  // Ensure they're inside the content area (within SidebarInset)
  const sidebarInset = screen.getByTestId('sidebar-inset');
  expect(sidebarInset).toContainElement(screen.getByTestId('test-child-1'));
  expect(sidebarInset).toContainElement(screen.getByTestId('test-child-2'));
});