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
    <div className="flex p-2.5 border-t border-gray-300 text-secondary-foreground ">
    <input
        type="text"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="How can we help you today?"
        onKeyDown={handleKeyDown}
        className="flex-1 p-2.5 rounded-sm mr-2.5 focus:outline-none focus:ring-2 focus:ring-muted/50"
    />
    <button
    type="button"
    disabled={!message}
    onClick={handleButton}
    className="py-2.5 px-5 bg-purple-700 hover:bg-purple-800 text-white border-0 rounded-lg cursor-pointer disabled:opacity-50 transition-all">
    Send
    </button>

    </div>

    );
}
