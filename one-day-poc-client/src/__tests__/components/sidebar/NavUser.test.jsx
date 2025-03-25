/* eslint-disable react/prop-types */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, test, expect, beforeEach, afterEach } from 'vitest';
import { NavUser } from '@/components/sidebar/NavUser';
import { useNavigate } from 'react-router-dom';
import { useSidebar } from '@/components/ui/Sidebar';

vi.mock('react-router-dom', () => ({
  useNavigate: vi.fn(),
}));

vi.mock('@/components/ui/Sidebar', () => ({
  SidebarMenu: ({ children }) => <div data-testid="sidebar-menu">{children}</div>,
  SidebarMenuButton: ({ children, size, className }) => (
    <button data-testid="sidebar-menu-button" data-size={size} className={className}>
      {children}
    </button>
  ),
  SidebarMenuItem: ({ children }) => <div data-testid="sidebar-menu-item">{children}</div>,
  useSidebar: vi.fn(),
}));

vi.mock('@/components/ui/Avatar', () => ({
  Avatar: ({ children, className }) => (
    <div data-testid="avatar" className={className}>
      {children}
    </div>
  ),
  AvatarImage: ({ src, alt }) => (
    <img data-testid="avatar-image" src={src} alt={alt} />
  ),
  AvatarFallback: ({ children, className }) => (
    <div data-testid="avatar-fallback" className={className}>
      {children}
    </div>
  ),
}));

vi.mock('@/components/ui/DropdownMenu', () => ({
  DropdownMenu: ({ children }) => <div data-testid="dropdown-menu">{children}</div>,
  DropdownMenuTrigger: ({ children, asChild }) => (
    <div data-testid="dropdown-menu-trigger" data-as-child={asChild}>
      {children}
    </div>
  ),
  DropdownMenuContent: ({ children, className, side, align, sideOffset }) => (
    <div
      data-testid="dropdown-menu-content"
      className={className}
      data-side={side}
      data-align={align}
      data-side-offset={sideOffset}
    >
      {children}
    </div>
  ),
  DropdownMenuGroup: ({ children }) => <div data-testid="dropdown-menu-group">{children}</div>,
  DropdownMenuItem: ({ children, className, onClick }) => (
    <div
      data-testid="dropdown-menu-item"
      className={className}
      onClick={onClick}
    >
      {children}
    </div>
  ),
  DropdownMenuLabel: ({ children, className }) => (
    <div data-testid="dropdown-menu-label" className={className}>
      {children}
    </div>
  ),
  DropdownMenuSeparator: () => <div data-testid="dropdown-menu-separator" />,
}));

vi.mock('lucide-react', () => ({
  BadgeCheck: () => <div data-testid="badge-check-icon">BadgeCheck</div>,
  LogOut: () => <div data-testid="logout-icon">LogOut</div>,
}));

vi.mock('@radix-ui/react-icons', () => ({
  CaretSortIcon: () => <div data-testid="caret-sort-icon">CaretSortIcon</div>,
}));

const originalFetch = window.fetch;

beforeEach(() => {
  vi.resetAllMocks();
  window.fetch = vi.fn();

  Object.defineProperty(window, 'location', {
    writable: true,
    value: { href: '' },
  });

  useSidebar.mockReturnValue({ isMobile: false });

  useNavigate.mockReturnValue(vi.fn());
});

afterEach(() => {
  window.fetch = originalFetch;
});

test('Fetches and displays user data', async () => {
  const mockUser = {
    name: 'John Doe',
    email: 'john@example.com',
    avatar: 'https://example.com/avatar.jpg',
  };

  window.fetch.mockResolvedValueOnce({
    json: () => Promise.resolve(mockUser),
  });

  render(<NavUser />);

  await waitFor(() => {
    expect(window.fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/auth/me',
      expect.objectContaining({
        method: 'GET',
        credentials: 'include',
      })
    );
  });

  await waitFor(() => {
    expect(screen.getAllByText('John Doe')[0]).toBeInTheDocument();
    expect(screen.getAllByText('john@example.com')[0]).toBeInTheDocument();
  });

  const avatarImages = screen.getAllByTestId('avatar-image');
  expect(avatarImages[0]).toHaveAttribute('src', 'https://example.com/avatar.jpg');
});

test('Navigates to profile page when Account is clicked', async () => {
  const mockUser = {
    name: 'John Doe',
    email: 'john@example.com',
    avatar: 'https://example.com/avatar.jpg',
  };

  window.fetch.mockResolvedValueOnce({
    json: () => Promise.resolve(mockUser),
  });

  const mockNavigate = vi.fn();
  useNavigate.mockReturnValue(mockNavigate);

  render(<NavUser />);

  await waitFor(() => {
    expect(screen.getAllByText('John Doe')[0]).toBeInTheDocument();
  });

  const accountItem = screen.getAllByTestId('dropdown-menu-item')[0];
  fireEvent.click(accountItem);

  expect(mockNavigate).toHaveBeenCalledWith('/profile');
});

test('Logs out user when Log out is clicked', async () => {
  const mockUser = {
    name: 'John Doe',
    email: 'john@example.com',
    avatar: 'https://example.com/avatar.jpg',
  };

  window.fetch.mockResolvedValueOnce({
    json: () => Promise.resolve(mockUser),
  });

  window.fetch.mockResolvedValueOnce({
    ok: true,
  });

  render(<NavUser />);

  await waitFor(() => {
    expect(screen.getAllByText('John Doe')[0]).toBeInTheDocument();
  });

  // Find the Log out menu item
  const logoutItems = screen.getAllByTestId('dropdown-menu-item');
  const logoutItem = logoutItems[logoutItems.length - 1];
  fireEvent.click(logoutItem);

  await waitFor(() => {
    expect(window.fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/auth/logout',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
      })
    );
  });

  expect(window.location.href).toBe('/');
});

test('Handles logout error gracefully', async () => {
  const mockUser = {
    name: 'John Doe',
    email: 'john@example.com',
    avatar: 'https://example.com/avatar.jpg',
  };

  window.fetch.mockResolvedValueOnce({
    json: () => Promise.resolve(mockUser),
  });

  const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
  window.fetch.mockRejectedValueOnce(new Error('Network error'));

  render(<NavUser />);

  await waitFor(() => {
    expect(screen.getAllByText('John Doe')[0]).toBeInTheDocument();
  });

  const logoutItems = screen.getAllByTestId('dropdown-menu-item');
  const logoutItem = logoutItems[logoutItems.length - 1];
  fireEvent.click(logoutItem);

  await waitFor(() => {
    expect(window.fetch).toHaveBeenCalledWith(
      'http://localhost:8000/api/auth/logout',
      expect.any(Object)
    );
  });

  expect(consoleErrorSpy).toHaveBeenCalled();

  consoleErrorSpy.mockRestore();
});

test('Adapts dropdown menu position on mobile', async () => {
  useSidebar.mockReturnValue({ isMobile: true });

  const mockUser = {
    name: 'John Doe',
    email: 'john@example.com',
    avatar: 'https://example.com/avatar.jpg',
  };

  window.fetch.mockResolvedValueOnce({
    json: () => Promise.resolve(mockUser),
  });

  render(<NavUser />);

  await waitFor(() => {
    expect(screen.getAllByText('John Doe')[0]).toBeInTheDocument();
  });

  const dropdownContent = screen.getByTestId('dropdown-menu-content');
  expect(dropdownContent).toHaveAttribute('data-side', 'bottom');
});

test('Renders sidebar menu button with correct attributes', async () => {
  const mockUser = {
    name: 'John Doe',
    email: 'john@example.com',
    avatar: 'https://example.com/avatar.jpg',
  };

  window.fetch.mockResolvedValueOnce({
    json: () => Promise.resolve(mockUser),
  });

  render(<NavUser />);

  const sidebarMenuButton = screen.getByTestId('sidebar-menu-button');
  expect(sidebarMenuButton).toHaveAttribute('data-size', 'lg');
  expect(sidebarMenuButton.className).toContain('data-[state=open]:bg-sidebar-accent');
});