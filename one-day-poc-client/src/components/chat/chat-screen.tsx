import * as React from "react";
import { useEffect } from "react";
import { ChatBox } from "./chat-box";
import { MessageBox } from "./messages-box";
import  ChatMessage  from "@/hooks/Chat";
import { ChatScreenProps } from "../../types/Types";
import { useConversation } from "@/contexts/ConversationContext";

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
const ChatScreen: React.FC<ChatScreenProps> = ({ showPrototype, setPrototype, setPrototypeFiles, initialMessage }) => {
    const { messages, loadingMessages, activeConversationId } = useConversation();
    
    const {
        message,
        setMessage,
        sentMessages,
        handleSend,
        errorMessage,
        setErrorMessage,
    } = ChatMessage({setPrototype, setPrototypeFiles});
    
    const combinedMessages = React.useMemo(() => {
        const allMessages = [...messages, ...sentMessages];
        
        const sortedMessages = allMessages.sort((a, b) => {
            const timeA = new Date(a.timestamp).getTime();
            const timeB = new Date(b.timestamp).getTime();
            return timeA - timeB;
        });
        
        const deduplicatedMessages = sortedMessages.reduce((acc, current, idx) => {
            if (idx === 0) {
                return [current];
            }
            
            const prev = acc[acc.length - 1];
            const isDuplicate = 
                prev.content === current.content && 
                prev.role === current.role &&
                Math.abs(new Date(prev.timestamp).getTime() - new Date(current.timestamp).getTime()) < 1000;
            
            if (!isDuplicate) {
                acc.push(current);
            }
            
            return acc;
        }, [] as typeof sortedMessages);
        
        return deduplicatedMessages;
    }, [messages, sentMessages]);

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
            {loadingMessages && <div className="absolute inset-0 flex items-center justify-center bg-background/80 z-10">
                <p>Loading messages...</p>
            </div>}
            
            <MessageBox sentMessages={combinedMessages} />
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
