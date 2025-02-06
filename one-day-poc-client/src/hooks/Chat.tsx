import { useState, useEffect } from 'react';
import { Message, MessagePayload, ChatHookReturn } from './types';

/*
 * Hook used to communicate to back end endpoint for chat messages
 */
const ChatMessage = (): ChatHookReturn => {
    const [message, setMessage] = useState<string>("");
    const [sentMessages, setSentMessages] = useState<Message[]>([]);
    const [llmResponse, setLlmResponse] = useState<string>("");

    const postMessage = async (message: string): Promise<string> => {
        try {
            const messagePayload: MessagePayload = {
                userID: "user123",  // hardcoded for now
                time: new Date().toISOString(), // ISO 8601 format
                prompt: message
            };

            const response = await fetch("http://localhost:8000/json", {
                method: 'POST',
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
            throw error;
        }
    };

    const handleSend = async (messageToSend: string = message): Promise<void> => {
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
            console.error('Failed to send message:', error);
            // handle error in UI (i [reza] am writing this so that i can hopefully ask someone front end on their opinion)
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
        llmResponse
    };
};

export default ChatMessage;