import React, {useEffect, useRef, useState} from "react";
import { TypographyMuted, TypographyInlineCode,} from "@/components/ui/typography"
import ChatMessage from "@/hooks/Chat";
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";


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
            <div className="flex flex-col flex-1 overflow-y-auto p-2.5 text-secondary-foreground">
                {/*List of messages*/}
                {sentMessages.map((msg, index) => (
                    msg.role === "User" ? (
                        <div
                            key={index}
                            className="p-2.5 bg-muted/90 rounded mb-2.5 self-end break-words max-w-[70%]"
                        >
                            
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

                            <TypographyMuted> {new Date(msg.timestamp).toLocaleString("en-GB",{
                                hour: "2-digit",
                                minute:"2-digit",
                                hour12: true
                            })} </TypographyMuted>
                        </div>
                    ) : (
                        <div
                            key={index}
                            className="p-2.5 bg-transparent rounded mb-2.5 self-start break-words w-full"
                        >
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

                            <TypographyMuted> {new Date(msg.timestamp).toLocaleString("en-GB",{
                                hour: "2-digit",
                                minute:"2-digit",
                                hour12: true
                            })} </TypographyMuted>
                        </div>
                    )
                ))}
                {/* Dummy div to enable auto-scrolling */}
                <div ref={recentMessageRef} />
            </div>
        </>
    )}
