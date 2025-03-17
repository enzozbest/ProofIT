import * as React from "react";
import { useState } from "react"

import { AppSidebar } from "@/components/sidebar/app-sidebar"
import { TypographySmall } from "@/components/ui/typography"


import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"

import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar"

import {
  Button
} from "@/components/ui/button"

import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

import { 
  ChevronDownIcon, 
} from "@radix-ui/react-icons"

import {
  Share,
  Rocket
} from "lucide-react";

/**
 * SidebarWrapper component provides the layout structure with a collapsible sidebar.
 * 
 * This component creates a full application layout with:
 * - A collapsible sidebar (AppSidebar) for navigation
 * - A header with project management controls including:
 *   - Project name with rename functionality
 *   - Export options dropdown
 *   - Deploy button
 * - A main content area where children are rendered
 * 
 * The component uses SidebarProvider to manage the sidebar state and ensure
 * all sidebar components have access to the shared context.
 * 
 * @returns {JSX.Element} A layout with sidebar, header, and content area
 */
export default function SidebarWrapper({children}:{children : React.ReactNode}) {

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-[[data-collapsible=icon]]/sidebar-wrapper:h-12 ">
          <div className="flex items-center gap-2 px-4 w-full">
            <SidebarTrigger className="-ml-1" />

            <div className="flex-1 flex items-center  justify-center gap-2">
              <Popover>
                <PopoverTrigger className="flex items-center gap-1 group bg-background ">
                  <TypographySmall>
                    Project name
                  </TypographySmall>
                  <ChevronDownIcon
                    className="transition-transform duration-200 group-data-[state=open]:rotate-180"
                  />
                </PopoverTrigger>
                <PopoverContent>
                <div className="flex flex-col space-y-1.5">
                  <Label htmlFor="name">Rename project</Label>
                  <Input id="name" placeholder="Name of your project" />
                </div>
                </PopoverContent>
              </Popover>
            </div>

            <div className="flex items-center gap-2 px-4">
              <div className="flex-1 flex items-center  justify-center gap-2">
                <Popover>
                  <PopoverTrigger className="flex items-center gap-2 group bg-background">
                    <Share size={14} />
                    <TypographySmall>
                      Export
                    </TypographySmall>
                    <ChevronDownIcon
                      className="transition-transform duration-200 group-data-[state=open]:rotate-180"
                    />
                  </PopoverTrigger>
                  <PopoverContent>
                  <div className="flex flex-col space-y-1.5">
                    <p> buttons tba</p>
                  </div>
                  </PopoverContent>
                </Popover>
                <Button className="gap-2">
                  <Rocket size={14}/>
                  Deploy
                  </Button>
              </div>
            </div>
          </div>
        </header>
        <div className="flex flex-1 gap-1 p-4 h-[calc(100vh-4rem)]">
            {children}
        </div>
         
 
      </SidebarInset>
    </SidebarProvider>
  )
}
