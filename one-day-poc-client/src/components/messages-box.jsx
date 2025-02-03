import React, {useEffect, useRef, useState} from "react";

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
                            <p>{msg[2]}</p>
                            <strong>User:</strong> {msg[1]}
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
                            <p>{msg[2]}</p>
                            <strong>LLM:</strong> {msg[1]}
                        </div>
                    )
                ))}
                {/* Dummy div to enable auto-scrolling */}
                <div ref={recentMessageRef} />
            </div>
        </>
    )}
