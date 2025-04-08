import {
  Message,
  ChatResponse,
  PrototypeResponse,
  ServerResponse,
  MessagePayload
} from '../types/Types';
import UserService from '../services/UserService';
import { fetchWithAuth } from './APIUtils';
import { createNewConversation } from './ConversationAPI';

type ChatCallback = (chatResponse: ChatResponse) => void;
type PrototypeCallback = (prototypeResponse: PrototypeResponse) => void;

export async function getConversationHistory(
  conversationId: string
): Promise<Message[]> {
  try {
    const response = await fetchWithAuth(
      `/chat/history/${conversationId}`
    );

    if (!response.ok) {
      throw new Error('Failed to fetch conversation history');
    }

    const messages = await response.json();

    console.log('Received message data:', messages);

    return Array.isArray(messages)
      ? messages.map((msg) => ({
          ...msg,
          role: msg.senderId === 'user' ? 'User' : 'LLM',
        }))
      : [];
  } catch (error) {
    console.error('Error fetching conversation history:', error);
    return [];
  }
}

/**
 * Sends a chat message to the server and processes the response.
 *
 * @param message - The user message to send to the server
 * @param onChatResponse - Callback function that handles chat response data
 * @param onPrototypeResponse - Callback function that handles prototype response data
 * @param isPredefined - boolean flag to control whether the prototype is served from hardcoded library or generated with LLM
 * @param onError - Optional callback function that handles error messages
 *
 * @returns A promise that resolves when the API call is complete
 */
export async function sendChatMessage(
  message: Message,
  onChatResponse: ChatCallback,
  onPrototypeResponse: PrototypeCallback,
  isPredefined: boolean = false,
  onError?: (message: string) => void
): Promise<void> {
  try {
    if (!message.conversationId) {
      message.conversationId = createNewConversation();
    }

    const messagePayload: MessagePayload = {
      userID: UserService.getUserId(),
      time: message.timestamp,
      prompt: message.content,
      conversationId: message.conversationId,
      predefined: isPredefined,
    };

    const response = await fetchWithAuth('/chat/json', {
      method: 'POST',
      body: JSON.stringify(messagePayload),
    });

    if (!response.ok) {
      if (response.status === 500) {
        const errorData = await response.text();
        if (onError) onError('There was an error, please try again');
        throw new Error(errorData);
      }
      throw new Error('Network response was not ok');
    }

    const serverResponse: ServerResponse = await response.json();

    console.log('Server response:', serverResponse);

    if (serverResponse.chat) {
      onChatResponse(serverResponse.chat);
    }

    if (serverResponse.prototype) {
      onPrototypeResponse(serverResponse.prototype);
    }
  } catch (error) {
    console.error('API Error:', error);
    if (onError) onError('There was an error, please try again');
    throw error;
  }
}