import { useState, useEffect } from 'react';
import { Message, MessagePayload, ChatHookReturn, ChatMessageProps } from './Types';


/*
 * Hook used to communicate to back end endpoint for chat messages
 */
const ChatMessage = ({ setPrototype, setPrototypeId, prototypeId }:ChatMessageProps): ChatHookReturn => {
    const [message, setMessage] = useState<string>("");
    const [sentMessages, setSentMessages] = useState<Message[]>([]);
    const [llmResponse, setLlmResponse] = useState<string>("");
    const [errorMessage, setErrorMessage] = useState<string | null>(null);


    const postMessage = async (message: string): Promise<string> => {
        try {
            const messagePayload: MessagePayload = {
                userID: "user123",  // hardcoded for now
                time: new Date().toISOString(), // ISO 8601 format
                prompt: message
            };

            const response = await fetch("http://localhost:8000/json", {
                method: 'POST',
                credentials: "include",
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(messagePayload)
            });

            if (!response.ok) {
                throw new Error('Network response was not ok');
            }

            const data = await response.text();
            setLlmResponse(data);
            return data;
        } catch (error) {
            console.error('Error:', error);
            setErrorMessage("Error. Please check your connection and try again.");
            throw error;
        }
    };

    const handleSend = async (messageToSend: string = message): Promise<void> => {
        if (!message.trim()) return;
        setPrototype(true);
        setPrototypeId(prototypeId+1);
        const currentTime = new Date().toLocaleString();

        const newMessage: Message = {
            role: 'User',
            content: messageToSend,
            timestamp: currentTime
        };

        setSentMessages((prevMessages) => [...prevMessages, newMessage]);

        try {
            await postMessage(messageToSend);
            setMessage("");
        } catch (error) {
            console.error('Error:', error);
            setErrorMessage("Error. Please check your connection and try again.");
            throw error;
            
        }
    };

    useEffect(() => {
        if (llmResponse) {
            const currentTime = new Date().toLocaleString();

            const newLLMMessage: Message = {
                role: 'LLM',
                content: llmResponse,
                timestamp: currentTime
            };

            setSentMessages((prevMessages) => [...prevMessages, newLLMMessage]);
        }
    }, [llmResponse]);

    return {
        message,
        setMessage,
        sentMessages,
        handleSend,
        llmResponse,
        errorMessage,
        setErrorMessage,

    };
};

export default ChatMessage;