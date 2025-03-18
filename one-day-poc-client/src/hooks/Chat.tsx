import { useState, useEffect } from 'react';
import { sendChatMessage } from '../api/FrontEndAPI';
import { Message, ChatHookReturn, ChatMessageProps } from '../types/Types';
import { useConversation } from '../contexts/ConversationContext';

const ChatMessage = ({
  setPrototype,
  setPrototypeFiles,
}: ChatMessageProps): ChatHookReturn => {
  const [message, setMessage] = useState<string>('');
  const [sentMessages, setSentMessages] = useState<Message[]>([]);
  const [llmResponse, setLlmResponse] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  
  const { activeConversationId, createConversation } = useConversation();

  useEffect(() => {
    setSentMessages([]);
  }, [activeConversationId]);

  const handleSend = async (messageToSend: string = message): Promise<void> => {
    if (!messageToSend.trim()) return;

    const currentTime = new Date().toISOString();
    
    const conversationId = activeConversationId || createConversation();

    const newMessage: Message = {
      role: 'User',
      content: messageToSend,
      timestamp: currentTime,
      conversationId: conversationId
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
        conversationId: activeConversationId || ''
      };

      setSentMessages((prevMessages) => [...prevMessages, newLLMMessage]);
    }
  }, [llmResponse, activeConversationId]);

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
