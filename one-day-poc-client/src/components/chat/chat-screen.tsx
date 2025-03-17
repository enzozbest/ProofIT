import * as React from "react";
import { useEffect } from "react";
import { ChatBox } from "./chat-box";
import { MessageBox } from "./messages-box";
import  ChatMessage  from "@/hooks/Chat";
import { ChatScreenProps } from "../../types/Types";

import {toast} from 'sonner'

/**
 * ChatScreen component serves as the main chat interface container.
 * 
 * Coordinates between the input interface (ChatBox) and the message display (MessageBox)
 * while managing the chat state and prototype generation through the ChatMessage hook.
 * 
 * @param showPrototype - Boolean flag to control visibility of the prototype panel
 * @param setPrototype - Function to update the prototype state in parent component
 * @param setPrototypeFiles - Function to update the prototype files in parent component
 * @param initialMessage - Optional initial message to be processed automatically
 * 
 * @returns A complete chat interface with message history and input box
 */
const ChatScreen: React.FC<ChatScreenProps> = ({ showPrototype, setPrototype, setPrototypeFiles, initialMessage }) =>{

    const {
        message,
        setMessage,
        sentMessages,
        handleSend,
        errorMessage,
        setErrorMessage,
    } = ChatMessage({setPrototype, setPrototypeFiles});

    /**
     * Display error messages as toast notifications when they occur
     */
    useEffect(() => {
        if (errorMessage) {
            toast.error(errorMessage, {
                onDismiss: () => setErrorMessage(""), 
                onAutoClose: () => setErrorMessage(""),
                closeButton: true
            });
        }
    }, [errorMessage]);

    /**
     * Process initialMessage if provided (typically from routing)
     * Automatically sends the message after a short delay
     */
    useEffect(() => {
        if (initialMessage) {
            setMessage(initialMessage);
            const timer = setTimeout(() => {
                handleSend(initialMessage);
                sessionStorage.removeItem('initialMessage');
            }, 500);
            
            return () => clearTimeout(timer);
        }
    }, [initialMessage]);

    return (
        <div className="relative h-full flex flex-col">

            <MessageBox sentMessages={sentMessages}/>
            <ChatBox
                setMessage={setMessage}
                message={message}
                handleSend={handleSend}
                setError={setErrorMessage}
            />

        </div>

    );
}

export default ChatScreen;
