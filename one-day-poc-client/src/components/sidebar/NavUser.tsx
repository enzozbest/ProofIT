'use client';

import { BadgeCheck, LogOut, User } from 'lucide-react';

import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/DropdownMenu';
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from '@/components/ui/Sidebar';
import { useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { CaretSortIcon } from '@radix-ui/react-icons';

/**
 * NavUser component renders the user profile section in the sidebar.
 *
 * Displays the current user's information and provides a dropdown menu with
 * various user account options such as profile settings, billing, notifications,
 * and sign out functionality. The component adapts its layout based on whether
 * it's being viewed on mobile or desktop.
 *
 * @returns {JSX.Element} A sidebar menu item with user profile and dropdown
 */
export function NavUser({}: {
  user: {
    name: string;
    email: string;
    avatar: string;
  };
}) {
  const { isMobile } = useSidebar();
  const navigate = useNavigate();
  const handleSignOut = () => {
    fetch('http://localhost:8000/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
    })
      .then(() => {
        window.location.href = '/';
      })
      .catch((error) => console.error('Error:', error));
  };
  const [user, setUser] = useState<{
    name: string;
    email: string;
    avatar: string;
  } | null>(null);

  useEffect(() => {
    fetch('http://localhost:8000/api/auth/me', {
      method: 'GET',
      credentials: 'include',
    })
      .then((response) => response.json())
      .then((response) => {
        console.log(response);
        setUser(response);
      })
      .catch((err) => {});
  }, []);

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
            >
              <Avatar className="h-8 w-8 rounded-lg">
                <User></User>
              </Avatar>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-semibold ">{user?.name}</span>
                <span className="truncate text-xs ">{user?.email}</span>
              </div>
              <CaretSortIcon className="ml-auto size-4" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-[--radix-dropdown-menu-trigger-width] min-w-56 rounded-lg"
            side={isMobile ? 'bottom' : 'right'}
            align="end"
            sideOffset={4}
          >
            <DropdownMenuLabel className="p-0 font-normal">
              <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                <Avatar className="h-8 w-8 rounded-lg">
                  <User></User>
                </Avatar>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-semibold ">{user?.name}</span>
                  <span className="truncate text-xs ">{user?.email}</span>
                </div>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem
                className="cursor-pointer"
                onClick={() => navigate('/profile')}
              >
                <BadgeCheck />
                <span>Account</span>
              </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              className="cursor-pointer"
              onClick={handleSignOut}
            >
              <LogOut />
              Log out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
