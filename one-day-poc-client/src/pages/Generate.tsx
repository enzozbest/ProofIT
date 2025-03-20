import ChatScreen from '../components/chat/ChatScreen';
import PrototypeFrame from '@/hooks/PrototypeFrame';
import * as React from 'react';
import { useState, useEffect } from 'react';
import { ChevronRightIcon } from 'lucide-react';
import BackgroundSVG from '../assets/background.svg';

import SidebarWrapper from '@/components/sidebar/SidebarWrapper';
import NavBar from '@/components/NavBar';

/**
 * Generate Page Component
 *
 * Renders the main prototype generation interface with:
 * - A collapsible chat panel for interacting with the AI
 * - A live prototype preview that updates based on the conversation
 * - Cross-origin isolation detection for WebContainer compatibility
 * - Support for initial messages passed through session storage
 *
 * The layout consists of a navbar, a sidebar navigation, a chat panel that can be toggled
 * visible/hidden, and the main prototype preview area.
 *
 * @component
 * @returns {JSX.Element} The complete prototype generation interface
 */
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
      data-testid="container"
    >
      <SidebarWrapper>
        <div
          className={`w-[450px] rounded-xl bg-white/15 backdrop-blur-xl transition-all duration-300 ease-in-out overflow-hidden ${
            isVisible ? 'opacity-100 max-w-[450px]' : 'opacity-0 max-w-0'
          }`}
          data-testid="chat"
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
        <div className="flex-1 h-full rounded-xl bg-white/15 backdrop-blur-lg">
          {showPrototype ? (
            <PrototypeFrame
              files={prototypeFiles}
              data-testid="prototype-frame"
            />
          ) : null}
        </div>
      </SidebarWrapper>
    </div>
  );
}
