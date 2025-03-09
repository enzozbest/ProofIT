import { useState, useEffect } from 'react';
import { Message, ChatHookReturn, ChatMessageProps, MessageRole } from './Types';
import { sendChatMessage } from '../api/FrontEndAPI';

const ChatMessage = ({ setPrototype }: ChatMessageProps): ChatHookReturn => {
    const [message, setMessage] = useState<string>("");
    const [sentMessages, setSentMessages] = useState<Message[]>([]);
    const [llmResponse, setLlmResponse] = useState<string>("");
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const handleSend = async (messageToSend: string = message): Promise<void> => {
        if (!messageToSend.trim()) return;
        
        const currentTime = new Date().toISOString();

        const newMessage: Message = {
            role: 'User',
            content: messageToSend,
            timestamp: currentTime
        };
        setSentMessages((prevMessages) => [...prevMessages, newMessage]);

        try {
            await sendChatMessage(
                newMessage,
                (chatResponse) => {
                    setLlmResponse(chatResponse.message);
                    setSentMessages(prev => [...prev, {
                        role: chatResponse.role,
                        content: chatResponse.message,
                        timestamp: chatResponse.timestamp
                    } as Message]);
                },
                () => {
                    setPrototype(true);
                }
            );
            
            setMessage("");
        } catch (error) {
            console.error('Error:', error);
            setErrorMessage("Error. Please check your connection and try again.");
        }
    };

    useEffect(() => {
        if (llmResponse) {
            const currentTime = new Date().toISOString();

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