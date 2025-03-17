"use client"

import * as React from "react"
import {
  Settings2,
  History,
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

// This is sample data.
const data = {
  navMain: [
    {
      title: "History",
      url: "#",
      icon: History,
      isActive: true,
      className: "bg-transparent",
      items: [
        {
          title: "Chat 1",
          url: "#",
        },
        {
          title: "Chat2",
          url: "#",
        },
        {
          title: "Chat3",
          url: "#",
        },
      ],
    },
  ],
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>, user) {
  return (
    <Sidebar collapsible="icon" className="bg-background/85" {...props}>
      <SidebarHeader>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
