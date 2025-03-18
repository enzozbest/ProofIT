import ChatScreen from '../components/chat/chat-screen';
import PrototypeFrame from '@/hooks/PrototypeFrame';
import * as React from 'react';
import { useState, useEffect } from 'react';
import { ChevronRightIcon } from 'lucide-react';

import SidebarWrapper from '@/components/sidebar/sidebar-wrapper';

/**
 * Generate Page Component
 * 
 * Renders the main prototype generation interface with:
 * - A collapsible chat panel for interacting with the AI
 * - A live prototype preview that updates based on the conversation
 * - Cross-origin isolation detection for WebContainer compatibility
 * - Support for initial messages passed through session storage
 * 
 * The layout consists of a sidebar navigation, a chat panel that can be toggled
 * visible/hidden, and the main prototype preview area.
 * 
 * @component
 * @returns {JSX.Element} The complete prototype generation interface
 */
export default function Page() {
  const [isVisible, setIsVisible] = useState<boolean>(true);
  const [showPrototype, setPrototype] = useState<boolean>(true); // this should be false once everything is working
  const [prototypeFiles, setPrototypeFiles] = useState<any>(null);
  const [initialMessage, setInitialMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!window.crossOriginIsolated) {
      const hasAttemptedReload =
        sessionStorage.getItem('attempted_isolation_reload') === 'true';

      if (!hasAttemptedReload) {
        sessionStorage.setItem('attempted_isolation_reload', 'true');
        console.log(
          'Cross-origin isolation not detected. Reloading page to apply headers...'
        );
        window.location.reload();
        return;
      } else {
        console.error(
          'Failed to enable cross-origin isolation after reload. Check server headers.'
        );
        sessionStorage.removeItem('attempted_isolation_reload');
      }
    } else {
      sessionStorage.removeItem('attempted_isolation_reload');
    }

    const savedMessage = sessionStorage.getItem('initialMessage');
    if (savedMessage) {
      setInitialMessage(savedMessage);
    }
  }, []);

  return (
    <SidebarWrapper>
      <div
        className={`w-[450px] h-full rounded-xl bg-muted/50 transition-all duration-300 ease-inn-out overflow-hidden ${
          isVisible ? 'max-w-[450px]' : 'opacity-0 max-w-0'
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
      <div className="flex-1 h-full rounded-xl bg-muted/50">
        {showPrototype ? <PrototypeFrame files={prototypeFiles} /> : null}
      </div>
    </SidebarWrapper>
  );
}
