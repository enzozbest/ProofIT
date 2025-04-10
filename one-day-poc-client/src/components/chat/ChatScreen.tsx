import * as React from 'react';
import { useEffect } from 'react';
import { ChatBox } from './ChatBox';
import { MessageBox } from './MessagesBox';
import ChatMessage from '@/hooks/Chat';
import { ChatScreenProps } from '../../types/Types';
import { useConversation } from '@/contexts/ConversationContext';

import { toast } from 'sonner';
import { FileTree } from '@/types/Types';

/**
 * ChatScreen component serves as the main chat interface container.
 *
 * Coordinates between the input interface (ChatBox) and the message display (MessageBox)
 * while managing the chat state and prototype generation through the ChatMessage hook.
 *
 * @param showPrototype - Boolean flag to control visibility of the prototype panel
 * @param setPrototype - Function to update the prototype state in parent component
 * @param setPrototypeFiles - Function to update the prototype files in parent component
 * @param initialMessage - Optional initial message to be processed automatically
 * @param isPredefined - flag to determine if LLM call is needed for prototype generation
 *
 * @returns A complete chat interface with message history and input box
 */
const ChatScreen: React.FC<ChatScreenProps> = ({
  showPrototype,
  setPrototype,
  setPrototypeFiles,
  initialMessage,
  isPredefined = false,
}) => {
  const { messages, loadingMessages, activeConversationId } = useConversation();

  const {
    message,
    setMessage,
    sentMessages,
    handleSend,
    errorMessage,
    setErrorMessage,
  } = ChatMessage({ setPrototype, setPrototypeFiles });

  const combinedMessages = React.useMemo(() => {
    const allMessages = [...messages, ...sentMessages];

    const sortedMessages = allMessages.sort((a, b) => {
      const timeA = new Date(a.timestamp).getTime();
      const timeB = new Date(b.timestamp).getTime();
      return timeA - timeB;
    });

    const deduplicatedMessages = sortedMessages.reduce(
      (acc, current, idx) => {
        if (idx === 0) {
          return [current];
        }

        const prev = acc[acc.length - 1];
        const isDuplicate =
          prev.content === current.content &&
          prev.role === current.role &&
          Math.abs(
            new Date(prev.timestamp).getTime() -
              new Date(current.timestamp).getTime()
          ) < 1000;

        if (!isDuplicate) {
          acc.push(current);
        }

        return acc;
      },
      [] as typeof sortedMessages
    );

    return deduplicatedMessages;
  }, [messages, sentMessages]);

  /**
   * Display error messages as toast notifications when they occur
   */
  useEffect(() => {
    if (errorMessage) {
      toast.error(errorMessage, {
        onDismiss: () => setErrorMessage(''),
        onAutoClose: () => setErrorMessage(''),
        closeButton: true,
      });
    }
  }, [errorMessage]);

  /**
   * Process initialMessage if provided (typically from routing)
   * Automatically sends the message after a short delay
   */
  useEffect(() => {
    if (initialMessage && isPredefined) {
      setMessage(initialMessage);
      const timer = setTimeout(() => {
        handleSend(initialMessage, isPredefined);
        sessionStorage.removeItem('initialMessage');
      }, 500);

      return () => clearTimeout(timer);
    }
  }, [initialMessage, isPredefined]);

  const handleLoadPrototype = (files: FileTree) => {
    setPrototype(true);
    setPrototypeFiles(files);
  };

  return (
    <div className="relative flex flex-col h-[calc(100vh-8rem)]">
      {loadingMessages && (
        <div className="absolute inset-0 flex items-center justify-center bg-background/80 z-10">
          <p>Loading messages...</p>
        </div>
      )}

      <MessageBox
        sentMessages={combinedMessages}
        onLoadPrototype={handleLoadPrototype}
      />
      <ChatBox
        setMessage={setMessage}
        message={message}
        handleSend={handleSend}
        setError={setErrorMessage}
      />
    </div>
  );
};

export default ChatScreen;
