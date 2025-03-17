import React, {useEffect, useRef, useState} from "react";
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { MessageBubble, MessageBubbleContent, MessageBubbleTimestamp } from "./message-bubble";


/**
 * MessageBox component displays the chat conversation history.
 * 
 * Renders a scrollable list of messages with support for Markdown formatting,
 * code highlighting, and automatic scrolling to the most recent message.
 * Messages are styled differently based on their sender (user vs LLM).
 * 
 * @component
 * @param {Object} props - Component properties
 * @param {Array<Object>} props.sentMessages - Array of message objects to display
 * @param {string} props.sentMessages[].role - The sender of the message ('User' or 'LLM')
 * @param {string} props.sentMessages[].content - The message content, supports Markdown
 * @param {string} props.sentMessages[].timestamp - ISO timestamp string for the message
 * 
 * @returns {JSX.Element} A scrollable container with formatted chat messages
 */
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
            <div className="flex flex-col flex-1 overflow-y-auto gap-1 p-1 text-secondary-foreground">
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
