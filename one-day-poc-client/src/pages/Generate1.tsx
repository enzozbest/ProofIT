
import { AppSidebar } from "@/components/app-sidebar"
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
  ChevronRightIcon,
} from "@radix-ui/react-icons"

import {
  Share,
  Rocket
} from "lucide-react";

export default function Page() {

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-[[data-collapsible=icon]]/sidebar-wrapper:h-12 ">
          <div className="flex items-center gap-2 px-4 w-full">
            <SidebarTrigger className="-ml-1" />

            <div className="flex-1 flex items-center  justify-center gap-2">
              <Popover>
                <PopoverTrigger className="flex items-center gap-1 group">
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
                  <PopoverTrigger className="flex items-center gap-2 group">
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
                <Button variant="secondary" className="gap-2">
                  <Rocket size={14}/>
                  Deploy
                  </Button>
              </div>
            </div>
          </div>
        </header>
        <div className="flex flex-1 gap-1 p-4 h-[calc(100vh-4rem)]">
    
          <div className="w-[450px] h-full rounded-xl bg-muted/50 border border-neutral-500">
            chatbot placeholder meow
          </div>
          <div className="flex h-full items-center justify-center">
            <ChevronRightIcon className="h-12 w-9 text-neutral-200" />
          </div>
          <div className="flex-1 h-full rounded-xl bg-neutral-200">
            prototype viewer placeholder
          </div>
        </div>
         
 
      </SidebarInset>
    </SidebarProvider>
  )
}
