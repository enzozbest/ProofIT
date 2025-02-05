import React, {useEffect, useRef, useState} from "react";
import { TypographyMuted,
    TypographyInlineCode,
 } from "@/components/ui/typography"
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";


export function MessageBox({ sentMessage }) {
    const recentMessageRef = useRef(null);

    // Scroll to the most recent message
    useEffect(() => {
        if (recentMessageRef.current && recentMessageRef.current.offsetParent !== null) {
            recentMessageRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [sentMessage]);
    

    return(
        <>
            {/* Message List */}
            <div
                style={{
                    flex: 1,
                    overflowY: "auto",
                    padding: "10px",
                    display: "flex",
                    flexDirection: "column",
                }}
            >
                {/*List of messages*/}
                {sentMessage.map((msg, index) => (
                    msg[0] === "User" ? (
                        <div
                            key={index}
                            style={{
                                padding: "10px",
                                backgroundColor: "#f1f1f1",
                                borderRadius: "5px",
                                marginBottom: "10px",
                                alignSelf: "flex-end",
                                wordWrap: "break-word",
                                maxWidth: "70%",
                            }}
                        >
                            
                            {msg[1] && (
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
                                    {msg[1]} 
                                </Markdown>
                            )}

                            <TypographyMuted> {new Date(msg[2]).toLocaleString("en-GB",{
                                hour: "2-digit",
                                minute:"2-digit",
                                hour12: true
                            })} </TypographyMuted>
                        </div>
                    ) : (
                        <div
                            key={index}
                            style={{
                                padding: "10px",
                                backgroundColor: "#f1f1f1",
                                borderRadius: "5px",
                                marginBottom: "10px",
                                alignSelf: "flex-start",
                                wordWrap: "break-word",
                                maxWidth: "70%",
                            }}
                        >
                            {msg[1] && (
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
                                    {msg[1]} 
                                </Markdown>
                            )}

                            <TypographyMuted> {new Date(msg[2]).toLocaleString("en-GB",{
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
