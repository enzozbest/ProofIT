/**
 * Message Bubble Components
 *
 * This file contains a set of components used to create chat message bubbles with
 * support for different visual styles based on the message sender (user vs LLM).
 * Uses class-variance-authority for styling variants.
 */
import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

/**
 * Styling variants for the main message bubble container
 * - llm: Messages from the AI assistant (left-aligned)
 * - user: Messages from the human user (right-aligned)
 */
const messageBubbleVariant = cva(
  'flex gap-2 items-end break-words relative group',
  {
    variants: {
      variant: {
        llm: 'self-start',
        user: 'self-end flex-row-reverse max-w-[60%]',
      },
      layout: {
        default: '',
      },
    },
    defaultVariants: {
      variant: 'llm',
      layout: 'default',
    },
  }
);

/**
 * Props for the MessageBubble component
 * Extends HTML div props and includes variant options
 */
interface MessageBubbleProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof messageBubbleVariant> {}

/**
 * MessageBubble component - The main container for a chat message
 * Automatically applies variant styling to child components
 *
 * @param variant - Type of message (llm or user)
 * @param layout - Layout style variant
 */
const MessageBubble = React.forwardRef<HTMLDivElement, MessageBubbleProps>(
  ({ className, variant, layout, children, ...props }, ref) => (
    <div
      className={cn(
        messageBubbleVariant({ variant, layout, className }),
        'relative group'
      )}
      ref={ref}
      {...props}
    >
      {React.Children.map(children, (child) =>
        React.isValidElement(child) && typeof child.type !== 'string'
          ? React.cloneElement(child, {
              variant,
              layout,
            } as React.ComponentProps<typeof child.type>)
          : child
      )}
    </div>
  )
);
MessageBubble.displayName = 'MessageBubble';

/**
 * Styling variants for the message content container
 * Applies different visual styles based on whether the message is from the user or LLM
 */
const messageBubbleContentVariants = cva('p-2 px-5 text-white', {
  variants: {
    variant: {
      llm: 'bg-transparent text-left rounded-r-lg rounded-tl-lg',
      user: 'bg-background/50 rounded-l-lg rounded-tr-lg text-right p-',
    },
    layout: {
      default: '',
    },
  },
  defaultVariants: {
    variant: 'llm',
    layout: 'default',
  },
});

/**
 * Props for the MessageBubbleContent component
 * Extends HTML div props and includes variant options
 */
interface MessageBubbleContentProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof messageBubbleContentVariants> {
  isLoading?: boolean;
}

/**
 * MessageBubbleContent component - Contains the actual message text
 *
 * @param variant - Type of message (llm or user)
 * @param layout - Layout style variant
 * @param isLoading - Optional flag to show loading state
 */
const MessageBubbleContent = React.forwardRef<
  HTMLDivElement,
  MessageBubbleContentProps
>(
  (
    { className, variant, layout, isLoading = false, children, ...props },
    ref
  ) => (
    <div
      className={cn(
        messageBubbleContentVariants({ variant, layout, className }),
        'break-words max-w-full whitespace-pre-wrap'
      )}
      ref={ref}
      {...props}
    >
      {children}
    </div>
  )
);
MessageBubbleContent.displayName = 'MessageBubbleContent';

/**
 * Props for the MessageBubbleTimestamp component
 * @property timestamp - ISO string timestamp to display
 */
interface MessageBubbleTimestampProps
  extends React.HTMLAttributes<HTMLDivElement> {
  timestamp: string;
}

/**
 * MessageBubbleTimestamp component - Displays the message timestamp
 * Formats the timestamp as a localized time string (hour:minute AM/PM)
 *
 * @param timestamp - ISO string timestamp to display
 */
const MessageBubbleTimestamp: React.FC<MessageBubbleTimestampProps> = ({
  timestamp,
  className,
  ...props
}) => (
  <div
    className={cn('text-xs mt-1 text-muted-foreground', className)}
    {...props}
  >
    {new Date(timestamp).toLocaleTimeString('en-GB', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    })}
  </div>
);

export {
  MessageBubble,
  MessageBubbleContent,
  MessageBubbleTimestamp,
  messageBubbleVariant,
  messageBubbleContentVariants,
};
