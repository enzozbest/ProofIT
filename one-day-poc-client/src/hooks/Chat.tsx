import { useState, useEffect, useCallback } from 'react';
import { sendChatMessage } from '@/api';
import {
  Message,
  ChatHookReturn,
  ChatMessageProps,
  ChatResponse,
} from '../types/Types';
import { useConversation } from '../contexts/ConversationContext';

const createMessage = (
  role: 'User' | 'LLM', 
  content: string, 
  conversationId: string, 
  options: Partial<Message> = {}
): Message => ({
  role,
  content,
  timestamp: new Date().toISOString(),
  conversationId,
  ...options
});

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

  const handleChatError = useCallback((errorMsg: string, conversationId: string) => {
    console.error('Chat Error:', errorMsg);
    
    if(conversationId === activeConversationId) {
      const errorSystemMessage = createMessage('LLM', errorMsg, conversationId, { isError: true });
      setSentMessages((prevMessages) => [...prevMessages, errorSystemMessage]);
      setErrorMessage('Error. Please check your connection and try again.');
    }
  }, [activeConversationId, setSentMessages, setErrorMessage]);

  const handleSend = useCallback(async (messageToSend: string = message, isPredefined: boolean = false): Promise<void> => {
    if (!messageToSend.trim()) return;

    const conversationId = activeConversationId || createConversation();
    const newMessage = createMessage('User', messageToSend, conversationId);

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
        (errorMsg) => handleChatError(errorMsg, conversationId)
      );
    } catch (error) {
      handleChatError('There was an error, please try again', conversationId);
    }
  }, [message, activeConversationId, createConversation, setMessage, setSentMessages, setChatResponse, handleChatError, setPrototype, setPrototypeFiles]);

  useEffect(() => {
    if (chatResponse) {
      const newLLMMessage = createMessage(
        'LLM', 
        chatResponse.message, 
        chatResponse.conversationId ?? 'unknown-conversation',
        { id: chatResponse.messageId, isError: false }
      );
      console.log('Chat response:', chatResponse);

      if (newLLMMessage.conversationId === activeConversationId) {
        console.log('Adding message to current conversation:', {
          messageConversationId: newLLMMessage.conversationId,
          activeConversationId
        });
        setSentMessages((prevMessages) => [...prevMessages, newLLMMessage]);
      } else {
        console.log('Message from different conversation, not displaying', {
          messageConversationId: newLLMMessage.conversationId,
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
