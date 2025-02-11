import React, { useState, useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
import ChatMessage from "@/hooks/Chat";

export function ChatBox({ message, setMessage, handleSend, setError}) {
    const location = useLocation();
    const initialMessage = location.state?.initialMessage;
    const shouldSend = useRef(false);

    /*
        * Set the message to the one from the landing page
        * Block handleSend() from running until the message has been set
     */
    useEffect(() => {
        if (initialMessage) {
            setMessage(initialMessage);
            shouldSend.current = true;
        }
    }, []);

    /*
        * The initial message has been set, now we can send the message
     */
    useEffect(() => {
        if (shouldSend.current && message === initialMessage) {
            shouldSend.current = false;
            handleSend();
        }
    }, [message, initialMessage]);

    const recentMessageRef = useRef(null);

    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            handleSend();
        }
    };

    const handleButton = () => {
        handleSend();
    }

    return (
        <div
            style={{
                display: "flex",
                padding: "10px",
                borderTop: "1px solid #ccc",
            }}
        >
            <input
                type="text"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="How can we help you today?"
                onKeyDown={handleKeyDown}
                style={{
                    flex: 1,
                    padding: "10px",
                    border: "1px solid #ccc",
                    borderRadius: "5px",
                    marginRight: "10px",
                }}
            />
            <button
                disabled = {!message}
                onClick={handleButton}
                style={{
                    padding: "10px 20px",
                    backgroundColor: "#007bff",
                    color: "white",
                    border: "none",
                    borderRadius: "5px",
                    cursor: "pointer",
                }}
            >
                Send
            </button>

        
        </div>
    );
}
