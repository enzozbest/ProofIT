import React, { useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";

interface ChatBoxProps {
    message: string;
    setMessage: (message: string) => void;
    handleSend: (messageToSend?: string) => Promise<void>;
    setError: (error: string | null) => void;
  }

/**
 * ChatBox component provides a text input interface for the chat functionality.
 * 
 * This component handles both manual user input and automatic sending of messages
 * that may be passed from other pages via routing.
 * 
 * @param {string} message - Current message text value
 * @param {Function} setMessage - State setter function for updating the message
 * @param {Function} handleSend - Function to process and send the current message
 * @param {Function} setError - Function to set error state if message sending fails
 * 
 * @returns {JSX.Element} A chat input box with send button
 */
export function ChatBox({ message, setMessage, handleSend, setError}: ChatBoxProps) {
    const location = useLocation();
    const initialMessage = location.state?.initialMessage;
    const shouldSend = useRef(false);

    useEffect(() => {
        if (initialMessage) {
            setMessage(initialMessage);
            shouldSend.current = true;
        }
    }, []);

    useEffect(() => {
        if (shouldSend.current && message === initialMessage) {
            shouldSend.current = false;
            handleSend();
        }
    }, [message, initialMessage]);

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
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
        type = "button"
      disabled={!message}
      onClick={handleButton}
      className="py-2.5 px-5 bg-secondary text-white border-0 rounded-sm cursor-pointer disabled:opacity-50"
    >
      Send
    </button>
    </div>

    );
}
