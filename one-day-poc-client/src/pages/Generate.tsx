import ChatScreen from '../components/chat/chat-screen'
import PrototypeFrame from "@/hooks/PrototypeFrame";
import * as React from "react";
import { useState } from "react"
import { ChevronRightIcon } from 'lucide-react';

import SidebarWrapper from '@/components/sidebar/sidebar-wrapper';

export default function Page() {
  const [isVisible, setIsVisible] = useState<boolean>(true);
  const [showPrototype, setPrototype] = useState<boolean>(false);
  const [prototypeId, setPrototypeId] = useState<number>(0);

  return (
    <SidebarWrapper>
      <div className= {`w-[450px] h-full rounded-xl bg-muted/50 transition-all duration-300 ease-inn-out overflow-hidden ${
                           isVisible ? "opacity-100 max-w-[450px]" : "opacity-0 max-w-0"}`}>
          <ChatScreen
              showPrototype={showPrototype}
              setPrototype={setPrototype}
          />
          </div>
          <div className="flex h-full items-center justify-center">
            <button 
              onClick={() => setIsVisible(!isVisible)}
              className="bg-transparent">
            <ChevronRightIcon className={`h-12 w-9 text-neutral-400 transition-transform duration-200 ${
                                        isVisible ? "rotate-180": "rotate-0"}`} />
            </button>
          </div>
          <div className="flex-1 h-full rounded-xl bg-muted/50">
            { showPrototype ? <PrototypeFrame /> : null }
          </div>
    </SidebarWrapper>
  )
}
