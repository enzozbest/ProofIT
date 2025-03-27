'use client';

import * as React from 'react';
import { History, PlusCircle } from 'lucide-react';

import { NavMain } from '@/components/sidebar/NavMain';
import { NavUser } from '@/components/sidebar/NavUser';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
  SidebarMenuButton
} from '@/components/ui/Sidebar';
import { useConversation } from '@/contexts/ConversationContext';
import { Button } from '@/components/ui/Button';

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const {
    conversations,
    activeConversationId,
    setActiveConversationId,
    createConversation,
  } = useConversation();

  const navMainData = [
    {
      title: 'History',
      url: '#',
      icon: History,
      isActive: true,
      className: 'bg-transparent',
      items: conversations.map((conversation) => ({
        title: conversation.name,
        url: '#',
        id: conversation.id,
        isActive: activeConversationId === conversation.id,
        onClick: () => setActiveConversationId(conversation.id),
      })),
    },
  ];

  return (
    <Sidebar collapsible="icon" className="bg-background/85" {...props}>
      <SidebarHeader></SidebarHeader>
      <SidebarContent>
        <div className="relative flex w-full min-w-0 flex-col p-2">
          <SidebarMenuButton 
              data-testid="new-chat-button"
              onClick={createConversation}
              tooltip={"New Chat"} 
              className={"bg-transparent w-full justify-center items-center flex min-w-0 gap-1 border border-input group-data-[collapsible=icon]:border-0"}>
                  {PlusCircle && <PlusCircle className=""/>}
                  <span>{"New Chat"}</span>
              
          </SidebarMenuButton>
        </div>
        <NavMain items={navMainData} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}
