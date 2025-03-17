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
    title: string
    url: string
    icon?: LucideIcon
    isActive?: boolean
    items?: {
      title: string
      url: string
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
                <SidebarMenuButton tooltip={item.title} className="">
                  {item.icon && <item.icon className=""/>}
                  <span>{item.title}</span>
                  <ChevronRightIcon className="ml-auto transition-transform duration-200  group-data-[state=open]/collapsible:rotate-90" />
                </SidebarMenuButton>
              </CollapsibleTrigger>
              <CollapsibleContent>
                <SidebarMenuSub className="">
                  {item.items?.map((subItem) => (
                    <SidebarMenuSubItem key={subItem.title} className="">
                      <SidebarMenuSubButton asChild className="">
                        <a href={subItem.url} className="">
                          <span className="">{subItem.title}</span>
                        </a>
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
