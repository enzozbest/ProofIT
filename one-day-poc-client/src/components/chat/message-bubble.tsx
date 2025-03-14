import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const messageBubbleVariant = cva(
  "flex gap-2 items-end break-words relative group",
  {
    variants: {
      variant: {
        llm: "self-start",
        user: "self-end flex-row-reverse max-w-[60%]",
      },
      layout: {
        default: "",
      },
    },
    defaultVariants: {
      variant: "llm",
      layout: "default",
    },
  },
);

interface MessageBubbleProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof messageBubbleVariant> {}

const MessageBubble = React.forwardRef<HTMLDivElement, MessageBubbleProps>(
  ({ className, variant, layout, children, ...props }, ref) => (
    <div
      className={cn(
        messageBubbleVariant({ variant, layout, className }),
        "relative group",
      )}
      ref={ref}
      {...props}
    >
      {React.Children.map(children, (child) =>
        React.isValidElement(child) && typeof child.type !== "string"
          ? React.cloneElement(child, {
              variant,
              layout,
            } as React.ComponentProps<typeof child.type>)
          : child,
      )}
    </div>
  ),
);
MessageBubble.displayName = "MessageBubble";


const messageBubbleContentVariants = cva("p-2 px-5 text-white", {
  variants: {
    variant: {
      llm:
        "bg-transparent text-left rounded-r-lg rounded-tl-lg",
      user: "bg-background/50 rounded-l-lg rounded-tr-lg text-right p-",
    },
    layout: {
      default: "",
    },
  },
  defaultVariants: {
    variant: "llm",
    layout: "default",
  },
});

interface MessageBubbleContentProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof messageBubbleContentVariants> {
  isLoading?: boolean;
}

const MessageBubbleContent = React.forwardRef<
  HTMLDivElement,
  MessageBubbleContentProps
>(
  (
    { className, variant, layout, isLoading = false, children, ...props },
    ref,
  ) => (
    <div
      className={cn(
        messageBubbleContentVariants({ variant, layout, className }),
        "break-words max-w-full whitespace-pre-wrap",
      )}
      ref={ref}
      {...props}
    >
      {children}
    </div>
  ),
);
MessageBubbleContent.displayName = "MessageBubbleContent";

interface MessageBubbleTimestampProps
  extends React.HTMLAttributes<HTMLDivElement> {
  timestamp: string;
}

const MessageBubbleTimestamp: React.FC<MessageBubbleTimestampProps> = ({
  timestamp,
  className,
  ...props
}) => (
  <div className={cn("text-xs mt-1 text-muted-foreground", className)} {...props}>
    {new Date(timestamp).toLocaleTimeString("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
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
