import ChatScreen from '../components/chat/chat-screen';
import PrototypeFrame from '@/hooks/PrototypeFrame';
import * as React from 'react';
import { useState, useEffect } from 'react';
import { ChevronRightIcon } from 'lucide-react';
import BackgroundSVG from '../assets/background.svg';

import SidebarWrapper from '@/components/sidebar/sidebar-wrapper';

export default function Page() {
  const [isVisible, setIsVisible] = useState<boolean>(true);
  const [showPrototype, setPrototype] = useState<boolean>(true);
  const [prototypeFiles, setPrototypeFiles] = useState<any>(null);
  const [initialMessage, setInitialMessage] = useState<string | null>(null);

  useEffect(() => {
    const savedMessage = sessionStorage.getItem('initialMessage');
    if (savedMessage) {
      setInitialMessage(savedMessage);
    }
  }, []);

  return (
    <div
      className="min-h-screen bg-gray-900 text-white"
      style={{
        backgroundImage: "url('/background.svg')",
        backgroundSize: 'cover',
        backgroundRepeat: 'no-repeat',
        backgroundPosition: 'center',
      }}
    >
      <SidebarWrapper>
        <div
          className={`w-[450px] h-full rounded-xl bg-white/15 backdrop-blur-xl transition-all duration-300 ease-in-out overflow-hidden ${
            isVisible ? 'opacity-100 max-w-[450px]' : 'opacity-0 max-w-0'
          }`}
        >
          <ChatScreen
            showPrototype={showPrototype}
            setPrototype={setPrototype}
            setPrototypeFiles={setPrototypeFiles}
            initialMessage={initialMessage}
          />
        </div>
        <div className="flex h-full items-center justify-center">
          <button
            data-testid="toggle-button"
            onClick={() => setIsVisible(!isVisible)}
            className="bg-transparent"
          >
            <ChevronRightIcon
              className={`h-12 w-9 text-neutral-400 transition-transform duration-200 ${
                isVisible ? 'rotate-180' : 'rotate-0'
              }`}
            />
          </button>
        </div>
        <div className="flex-1 h-full rounded-xl bg-gray-900/60 backdrop-blur-lg">
          {showPrototype ? <PrototypeFrame files={prototypeFiles} /> : null}
        </div>
      </SidebarWrapper>
    </div>
  );
}
