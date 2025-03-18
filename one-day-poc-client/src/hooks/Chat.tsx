import { useState, useEffect } from 'react';
import { sendChatMessage } from '../api/FrontEndAPI';
import { Message, ChatHookReturn, ChatMessageProps } from '../types/Types';

/**
 * ChatMessage hook manages state and functionality for chat interactions
 * 
 * This hook handles:
 * - Managing input message state
 * - Storing conversation history
 * - Sending messages to the chat API
 * - Processing text responses from the LLM
 * - Handling prototype file generation
 * - Error handling for API interactions
 * 
 * @param {Object} props - Hook properties
 * @param {Function} props.setPrototype - Function to update prototype visibility state
 * @param {Function} props.setPrototypeFiles - Function to update prototype file content
 * 
 * @returns {ChatHookReturn} Object containing chat state and functions
 */
const ChatMessage = ({
  setPrototype,
  setPrototypeFiles,
}: ChatMessageProps): ChatHookReturn => {
  const [message, setMessage] = useState<string>('');
  const [sentMessages, setSentMessages] = useState<Message[]>([]);
  const [llmResponse, setLlmResponse] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSend = async (messageToSend: string = message): Promise<void> => {
    if (!messageToSend.trim()) return;

    const currentTime = new Date().toISOString();

    const newMessage: Message = {
      role: 'User',
      content: messageToSend,
      timestamp: currentTime,
    };
    setSentMessages((prevMessages) => [...prevMessages, newMessage]);
    setMessage('');
    try {
      await sendChatMessage(
        newMessage,
        (chatResponse) => {
          setLlmResponse(chatResponse.message);
        },
        (prototypeResponse) => {
          setPrototype(true);
          setPrototypeFiles(prototypeResponse.files);
        }
      );
    } catch (error) {
      console.error('Error:', error);
      setErrorMessage('Error. Please check your connection and try again.');
    }
  };

  useEffect(() => {
    if (llmResponse) {
      const currentTime = new Date().toISOString();

      const newLLMMessage: Message = {
        role: 'LLM',
        content: llmResponse,
        timestamp: currentTime,
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
