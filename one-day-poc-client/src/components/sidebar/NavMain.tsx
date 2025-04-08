'use client';

import React from 'react';
import { ChevronRight as ChevronRightIcon, LucideIcon } from 'lucide-react';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/Collapsible';
import {
  SidebarGroup,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarMenuSub,
  SidebarMenuSubItem,
  SidebarMenuSubButton,
} from '@/components/ui/Sidebar';

/**
 * NavMain component renders a collapsible sidebar navigation menu.
 *
 * The component creates a hierarchical navigation structure with collapsible
 * main items that can contain nested sub-items. Each main item can have an
 * optional icon and can be set as active by default.
 *
 * @returns {JSX.Element} A sidebar navigation menu with collapsible sections
 */
export function NavMain({
  items,
}: {
  items: {
    title: string;
    url: string;
    icon?: LucideIcon;
    isActive?: boolean;
    className?: string;
    items?: {
      title: string;
      url: string;
      id?: string;
      isActive?: boolean;
      subtitle?: string;
      onClick?: () => void;
      actions?: {
        icon: LucideIcon;
        className?: string;
        onClick: (e: React.MouseEvent) => void;
      }[];
    }[];
  }[];
}) {
  return (
    <SidebarGroup>
      <SidebarMenu className="">
        {items.map((item) => (
          <Collapsible
            key={item.title}
            asChild
            defaultOpen={item.isActive}
            className="group/collapsible"
          >
            <SidebarMenuItem className="">
              <CollapsibleTrigger asChild>
                <SidebarMenuButton
                  tooltip={item.title}
                  className={item.className || ''}
                >
                  {item.icon && <item.icon className="" />}
                  <span>{item.title}</span>
                  <ChevronRightIcon className="ml-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90" />
                </SidebarMenuButton>
              </CollapsibleTrigger>
              <CollapsibleContent>
                <SidebarMenuSub className="max-h-[300px] overflow-y-auto pr-1">
                  {item.items?.map((subItem) => (
                    <SidebarMenuSubItem
                      key={subItem.id || subItem.title}
                      className="group relative"
                    >
                      <SidebarMenuSubButton
                        onClick={(e) => {
                          e.preventDefault();
                          if (subItem.onClick) subItem.onClick();
                        }}
                        className={`w-full ${subItem.isActive ? 'bg-muted text-foreground' : ''}`}
                      >
                        <div className="flex w-full items-center justify-between">
                          <div className="truncate" title={subItem.title}>
                            {subItem.title}
                          </div>
                          
                          {subItem.actions && (
                            <div className="ml-2 flex items-center opacity-0 group-hover:opacity-100 transition-opacity">
                              {subItem.actions.map((action, index) => (
                                <button
                                  key={index}
                                  onClick={action.onClick}
                                  className={`p-0.5 rounded-sm mr-1 ${action.className || ""}`}
                                  aria-label="Delete conversation"
                                >
                                  <action.icon className="w-3.5 h-3.5" />
                                </button>
                              ))}
                            </div>
                          )}
                        </div>
                        
                        {subItem.subtitle && (
                          <span className="text-xs text-muted-foreground mt-1 truncate w-full">
                            {subItem.subtitle}
                          </span>
                        )}
                      </SidebarMenuSubButton>
                    </SidebarMenuSubItem>
                  ))}
                </SidebarMenuSub>
              </CollapsibleContent>
            </SidebarMenuItem>
          </Collapsible>
        ))}
      </SidebarMenu>
    </SidebarGroup>
  );
}
