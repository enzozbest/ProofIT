"use client"

import { type LucideIcon } from "lucide-react"

import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible"
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar"
import { ChevronRightIcon } from "@radix-ui/react-icons"

export function NavMain({
  items,
}: {
  items: {
    title: string
    url: string
    icon?: LucideIcon
    isActive?: boolean
    className?: string
    items?: {
      title: string
      url: string
      id?: string
      isActive?: boolean
      subtitle?: string
      onClick?: () => void
    }[]
  }[]
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
                <SidebarMenuButton tooltip={item.title} className={item.className || ""}>
                  {item.icon && <item.icon className=""/>}
                  <span>{item.title}</span>
                  <ChevronRightIcon className="ml-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90" />
                </SidebarMenuButton>
              </CollapsibleTrigger>
              <CollapsibleContent>
                <SidebarMenuSub className="">
                  {item.items?.map((subItem) => (
                    <SidebarMenuSubItem key={subItem.id || subItem.title} className="">
                      <SidebarMenuSubButton 
                        onClick={(e) => {
                          e.preventDefault();
                          if (subItem.onClick) subItem.onClick();
                        }}
                        className={`w-full ${subItem.isActive ? "bg-muted text-foreground" : ""}`}
                      >
                        <span className="truncate w-full" title={subItem.title}>
                          {subItem.title}
                        </span>
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
  )
}
