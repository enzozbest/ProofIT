import React, { useEffect, useRef } from 'react';
import Markdown from 'react-markdown';
import { Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {
  MessageBubble,
  MessageBubbleContent,
  MessageBubbleTimestamp,
} from './MessageBubble';
import { Message, MessageBoxProps } from '@/types/Types';
import { getPrototypeForMessage } from '@/api/FrontEndAPI';

/**
 * MessageBox component displays the chat conversation history.
 *
 * Renders a scrollable list of messages with support for Markdown formatting,
 * code highlighting, and automatic scrolling to the most recent message.
 * Messages are styled differently based on their sender (user vs LLM).
 */
export function MessageBox({
  sentMessages,
  onLoadPrototype,
  onMessageClick,
}: MessageBoxProps): JSX.Element {
  const recentMessageRef = useRef<HTMLDivElement>(null);

  // Scroll to the most recent message
  useEffect(() => {
    if (
      recentMessageRef.current &&
      recentMessageRef.current.offsetParent !== null
    ) {
      recentMessageRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [sentMessages]);

  const handleMessageClick = async (msg: Message): Promise<void> => {
    if (msg.role !== 'LLM' || !msg.conversationId || !msg.id) return;

    try {
      const prototypeFiles = await getPrototypeForMessage(
        msg.conversationId,
        msg.id
      );
      if (prototypeFiles) {
        onLoadPrototype(prototypeFiles);
      }
    } catch (error) {
      console.error('Error loading prototype:', error);
    }
  };

  const markdownComponents: Components = {
    code({
      inline,
      className,
      children,
      ...props
    }: React.HTMLAttributes<HTMLElement> & { inline?: boolean }) {
      return inline ? (
        <code
          className="inline-block bg-muted px-1 py-0.5 rounded font-mono font-semibold text-sm"
          {...props}
        >
          {children}
        </code>
      ) : (
        <pre className="whitespace-pre-wrap pt-2">
          <code
            className="block bg-muted rounded p-2 font-mono text-sm"
            {...props}
          >
            {children}
          </code>
        </pre>
      );
    },
  };

  return (
    <>
      <div className="flex flex-col flex-1 overflow-y-auto gap-1 p-1 text-secondary-foreground no-scrollbar">
        {sentMessages.map((msg, index) => (
          <MessageBubble
            key={index}
            variant={msg.role === 'User' ? 'user' : 'llm'}
            className={`bg-gray-800/40 text-white rounded-xl border border-gray-700/50 ${
              msg.role === 'LLM'
                ? 'cursor-pointer hover:border-blue-400 hover:bg-gray-700/40'
                : ''
            }`}
            onClick={() => {
              if (!msg.isError && msg.role === 'LLM') {
                onMessageClick?.(msg);
                handleMessageClick(msg);
              }
            }}
            data-testid={`message-${msg.id || index}`}
          >
            {/* Display a code icon for LLM messages */}
            {!msg.isError && msg.role === 'LLM' && (
              <div className="absolute -top-1 -left-1 bg-blue-500 rounded-full p-1 text-xs text-white">
                <span role="img" aria-label="code">
                  💻
                </span>
              </div>
            )}

            <MessageBubbleContent data-testid="message-bubble">
              {msg.content && (
                <Markdown
                  key={index}
                  remarkPlugins={[remarkGfm]}
                  components={markdownComponents}
                >
                  {msg.content}
                </Markdown>
              )}
              <MessageBubbleTimestamp timestamp={msg.timestamp} />
            </MessageBubbleContent>
          </MessageBubble>
        ))}
        <div ref={recentMessageRef} />
      </div>
    </>
  );
}
