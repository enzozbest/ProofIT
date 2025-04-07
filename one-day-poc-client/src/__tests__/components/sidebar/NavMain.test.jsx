/* eslint-disable react/prop-types */
import { render, screen, fireEvent } from '@testing-library/react';
import { vi, test, expect, beforeEach } from 'vitest';
import { NavMain } from '@/components/sidebar/NavMain';

vi.mock('@/components/ui/Collapsible', () => ({
  Collapsible: ({ children, asChild, defaultOpen, className }) => (
    <div
      data-testid="collapsible"
      data-default-open={defaultOpen}
      className={className}
    >
      {asChild ? children : null}
    </div>
  ),
  CollapsibleTrigger: ({ children, asChild }) => (
    <div data-testid="collapsible-trigger">{asChild ? children : null}</div>
  ),
  CollapsibleContent: ({ children }) => (
    <div data-testid="collapsible-content">{children}</div>
  ),
}));

vi.mock('@/components/ui/Sidebar', () => ({
  SidebarGroup: ({ children }) => (
    <div data-testid="sidebar-group">{children}</div>
  ),
  SidebarGroupLabel: ({ children }) => (
    <div data-testid="sidebar-group-label">{children}</div>
  ),
  SidebarMenu: ({ children, className }) => (
    <div data-testid="sidebar-menu" className={className}>
      {children}
    </div>
  ),
  SidebarMenuButton: ({ children, tooltip, className }) => (
    <button
      data-testid="sidebar-menu-button"
      data-tooltip={tooltip}
      className={className}
    >
      {children}
    </button>
  ),
  SidebarMenuItem: ({ children, className }) => (
    <div data-testid="sidebar-menu-item" className={className}>
      {children}
    </div>
  ),
  SidebarMenuSub: ({ children, className }) => (
    <div data-testid="sidebar-menu-sub" className={className}>
      {children}
    </div>
  ),
  SidebarMenuSubButton: ({ children, className, onClick }) => (
    <button
      data-testid="sidebar-menu-sub-button"
      className={className}
      onClick={onClick}
    >
      {children}
    </button>
  ),
  SidebarMenuSubItem: ({ children, className }) => (
    <div data-testid="sidebar-menu-sub-item" className={className}>
      {children}
    </div>
  ),
}));

vi.mock('lucide-react', () => ({
  ChevronRight: () => (
    <div data-testid="chevron-right-icon">ChevronRightIcon</div>
  ),
}));

const MockIcon = () => <div data-testid="mock-icon">MockIcon</div>;

beforeEach(() => {
  vi.resetAllMocks();
  vi.resetModules();
});

test('Renders NavMain with minimal props', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByTestId('sidebar-group')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-menu')).toBeInTheDocument();
  expect(screen.getByTestId('collapsible')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-menu-item')).toBeInTheDocument();
  expect(screen.getByTestId('collapsible-trigger')).toBeInTheDocument();
  expect(screen.getByTestId('sidebar-menu-button')).toBeInTheDocument();
  expect(screen.getByText('Item 1')).toBeInTheDocument();
  expect(screen.getByTestId('chevron-right-icon')).toBeInTheDocument();
  expect(screen.getByTestId('collapsible-content')).toBeInTheDocument();
});

test('Renders NavMain with multiple items', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
    },
    {
      title: 'Item 2',
      url: '#',
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getAllByTestId('collapsible')).toHaveLength(2);
  expect(screen.getByText('Item 1')).toBeInTheDocument();
  expect(screen.getByText('Item 2')).toBeInTheDocument();
});

test('Renders NavMain with icons', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      icon: MockIcon,
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByTestId('mock-icon')).toBeInTheDocument();
});

test('Renders NavMain with custom className for main items', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      className: 'custom-class',
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByTestId('sidebar-menu-button')).toHaveAttribute(
    'class',
    'custom-class'
  );
});

test('Renders NavMain with isActive for main items', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      isActive: true,
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByTestId('collapsible')).toHaveAttribute(
    'data-default-open',
    'true'
  );
});

test('Renders NavMain with sub-items', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
        },
        {
          title: 'Sub Item 2',
          url: '#',
        },
      ],
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByTestId('sidebar-menu-sub')).toBeInTheDocument();
  expect(screen.getAllByTestId('sidebar-menu-sub-item')).toHaveLength(2);
  expect(screen.getByText('Sub Item 1')).toBeInTheDocument();
  expect(screen.getByText('Sub Item 2')).toBeInTheDocument();
});

test('Renders NavMain with isActive for sub-items', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
          isActive: true,
        },
      ],
    },
  ];

  render(<NavMain items={items} />);

  const subButton = screen.getByTestId('sidebar-menu-sub-button');
  expect(subButton.className).toContain('bg-muted text-foreground');
});

test('Renders NavMain with subtitle for sub-items', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
          subtitle: 'Subtitle 1',
        },
      ],
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByText('Subtitle 1')).toBeInTheDocument();
  expect(screen.getByText('Subtitle 1').className).toContain(
    'text-xs text-muted-foreground mt-1 truncate w-full'
  );
});

test('Sub-item without onClick handler does not throw error', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
        },
      ],
    },
  ];

  render(<NavMain items={items} />);

  const subButton = screen.getByTestId('sidebar-menu-sub-button');

  fireEvent.click(subButton);
  expect(true).toBe(true);
});

test('Renders NavMain with ID for sub-items', () => {
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
          id: 'sub-item-1',
        },
      ],
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByText('Sub Item 1')).toBeInTheDocument();
});

test('Sub-item with onClick handler calls the handler when clicked', () => {
  const mockOnClick = vi.fn();
  
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
          onClick: mockOnClick, 
        },
      ],
    },
  ];

  render(<NavMain items={items} />);

  const subButton = screen.getByTestId('sidebar-menu-sub-button');
  
  expect(mockOnClick).not.toHaveBeenCalled();
  
  fireEvent.click(subButton);
  
  expect(mockOnClick).toHaveBeenCalledTimes(1);
});

test('Renders action buttons for sub-items', () => {
  const mockActionClick = vi.fn();
  const MockActionIcon = () => <div data-testid="action-icon">Action</div>;
  
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
          actions: [
            {
              icon: MockActionIcon,
              className: 'test-class',
              onClick: mockActionClick
            }
          ]
        },
      ],
    },
  ];

  render(<NavMain items={items} />);

  expect(screen.getByTestId('action-icon')).toBeInTheDocument();
});

test('Action button click calls handler without triggering parent onClick', () => {
  const mockItemClick = vi.fn();
  const mockActionClick = vi.fn();
  
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
          onClick: mockItemClick,
          actions: [
            {
              icon: () => <button 
                data-testid="action-button"
                onClick={(e) => {
                  e.stopPropagation();
                  mockActionClick();
                }}
              >
                Action
              </button>,
              onClick: (e) => {
                e.stopPropagation();
                mockActionClick();
              }
            }
          ]
        },
      ],
    },
  ];

  render(<NavMain items={items} />);
  
  const actionButton = screen.getByTestId('action-button');
  fireEvent.click(actionButton);
  
  expect(mockActionClick).toHaveBeenCalled();
  expect(mockItemClick).not.toHaveBeenCalled();
});

test('Action buttons are rendered with correct classes', () => {
  const mockActionClick = vi.fn();
  
  const items = [
    {
      title: 'Item 1',
      url: '#',
      items: [
        {
          title: 'Sub Item 1',
          url: '#',
          actions: [
            {
              icon: () => <div data-testid="action-icon">Action</div>,
              className: 'custom-action-class',
              onClick: mockActionClick
            }
          ]
        },
      ],
    },
  ];

  const { container } = render(<NavMain items={items} />);
  
  const actionButton = screen.getByTestId('action-icon').closest('button');
  
  expect(actionButton).toBeDefined();
  expect(actionButton.className).toContain('custom-action-class');
});