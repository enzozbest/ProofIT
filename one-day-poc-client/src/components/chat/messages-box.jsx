import React, {useEffect, useRef, useState} from "react";
import { TypographyMuted, TypographyInlineCode,} from "@/components/ui/typography"
import ChatMessage from "@/hooks/Chat";
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { MessageBubble, MessageBubbleContent, MessageBubbleTimestamp } from "./message-bubble";


export function MessageBox({ sentMessages }) {
    const recentMessageRef = useRef(null);


    // Scroll to the most recent message
    useEffect(() => {
        if (recentMessageRef.current && recentMessageRef.current.offsetParent !== null) {
            recentMessageRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [sentMessages]);
    

    return(
        <>
            {/* Message List */}
            <div className="flex flex-col flex-1 overflow-y-auto gap-1 p-1 text-secondary-foreground">
                {/*List of messages*/}
                {sentMessages.map((msg, index) => (
                    <MessageBubble
                    key={index}
                    variant={msg.role == "User" ? "user" : "llm"}
                    >
                        <MessageBubbleContent>
                        {msg.content && (
                            <Markdown
                                key={index}
                                remarkPlugins={[remarkGfm]} 
                                components={{
                                    code({ node, inline, className, children, ...props }) {
                                        return inline ? (
                                            <TypographyInlineCode {...props}>{children}</TypographyInlineCode>
                                        ) : (
                                            <pre className="whitespace-pre-wrap pt-2">
                                                <TypographyInlineCode {...props}>{children}</TypographyInlineCode>
                                            </pre>
                                        );
                                    }
                                }}
                            >
                                {msg.content}
                            </Markdown>
                        )}

                            <MessageBubbleTimestamp timestamp={msg.timestamp} />
                        </MessageBubbleContent>
                   </MessageBubble>
                    )
                )}
                {/* Dummy div to enable auto-scrolling */}
                <div ref={recentMessageRef} />
            </div>
        </>
    )}
