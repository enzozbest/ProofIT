"use client"

import * as React from "react"
import {
  History,
  PlusCircle
} from "lucide-react"

import { NavMain } from "@/components/sidebar/nav-main"
import { NavUser } from "@/components/sidebar/nav-user"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
} from "@/components/ui/sidebar"
import { useConversation } from "@/contexts/ConversationContext"
import { Button } from "@/components/ui/button"

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { conversations, activeConversationId, setActiveConversationId, createConversation } = useConversation();
  
  const navMainData = [
    {
      title: "History",
      url: "#",
      icon: History,
      isActive: true,
      className: "bg-transparent",
      items: conversations.map(conversation => ({
        title: conversation.name,
        url: "#",
        id: conversation.id,
        isActive: activeConversationId === conversation.id,
        onClick: () => setActiveConversationId(conversation.id)
      })),
    },
  ];

  return (
    <Sidebar collapsible="icon" className="bg-background/85" {...props}>
      <SidebarHeader>
        <div className="px-4 py-2">
          <Button 
            onClick={createConversation} 
            variant="outline" 
            className="w-full flex items-center"
          >
            <PlusCircle size={16} className="mr-2" />
            New Chat
          </Button>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={navMainData} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
