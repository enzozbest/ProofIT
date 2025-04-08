import { useState, useEffect } from 'react';
import { sendChatMessage } from '@/api';
import {
  Message,
  ChatHookReturn,
  ChatMessageProps,
  ChatResponse,
} from '../types/Types';
import { useConversation } from '../contexts/ConversationContext';

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
  const [chatResponse, setChatResponse] = useState<ChatResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    activeConversationId,
    createConversation,
    messages,
    loadingMessages,
  } = useConversation();

  useEffect(() => {
    if (messages.length > 0) {
      setSentMessages(messages);
    } else {
      setSentMessages([]);
    }
  }, [messages]);

  useEffect(() => {
    setSentMessages([]);
    setChatResponse(null);
  }, [activeConversationId]);

  const handleSend = async (messageToSend: string = message, isPredefined: boolean = false): Promise<void> => {
    if (!messageToSend.trim()) return;

    const currentTime = new Date().toISOString();

    const conversationId = activeConversationId || createConversation();

    const newMessage: Message = {
      role: 'User',
      content: messageToSend,
      timestamp: currentTime,
      conversationId: conversationId,
    };

    setSentMessages((prevMessages) => [...prevMessages, newMessage]);
    setMessage('');

    try {
      console.log("handleSend, isPredefined value is ", isPredefined);
      await sendChatMessage(
        newMessage,
        (chatResponse) => {
          setChatResponse(chatResponse);
        },
        (prototypeResponse) => {
          setPrototype(true);
          setPrototypeFiles(prototypeResponse.files);
        },
        isPredefined,
        (errorMsg) => {
          const errorSystemMessage: Message = {
            role: 'LLM',
            content: errorMsg,
            timestamp: new Date().toISOString(),
            conversationId: conversationId,
            isError: true,
          };
          setSentMessages((prevMessages) => [
            ...prevMessages,
            errorSystemMessage,
          ]);
          setErrorMessage('Error. Please check your connection and try again.');
        }
      );
    } catch (error) {
      console.error('Error:', error);
      setErrorMessage('Error. Please check your connection and try again.');
    }
  };

  useEffect(() => {
    if (chatResponse) {
      const currentTime = new Date().toISOString();
      console.log('Chat response:', chatResponse);

      const messageContent = chatResponse.message;
      const messageId = chatResponse.messageId;
      const conversationId = chatResponse.conversationId;

      const newLLMMessage: Message = {
        role: 'LLM',
        content: messageContent,
        timestamp: currentTime,
        conversationId: conversationId,
        id: messageId,
        isError: false,
      };

      if (newLLMMessage.conversationId === activeConversationId) {
        console.log('Adding message to current conversation:', {
          messageConversationId: conversationId,
          activeConversationId
        });
        setSentMessages((prevMessages) => [...prevMessages, newLLMMessage]);
      } else {
        console.log('Message from different conversation, not displaying', {
          messageConversationId: conversationId,
          activeConversationId
        });
      }
    }
  }, [chatResponse, activeConversationId]);

  return {
    message,
    setMessage,
    sentMessages,
    handleSend,
    errorMessage,
    setErrorMessage,
  };
};

export default ChatMessage;
