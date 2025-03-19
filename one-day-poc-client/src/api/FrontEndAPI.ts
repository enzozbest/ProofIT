import { Message, ChatResponse, PrototypeResponse, ServerResponse, MessagePayload, Conversation, ConversationHistory } from "../types/Types";
import UserService from "../services/UserService";
import { v4 as uuidv4 } from 'uuid';

type ChatCallback = (chatResponse: ChatResponse) => void;
type PrototypeCallback = (prototypeResponse: PrototypeResponse) => void;

export async function fetchChatHistory(): Promise<Conversation[]> {
  try {
    if (!UserService.getUser()) {
      console.log("Cannot fetch chat history as user is not logged in");
      return [];
    }
    console.log('Fetching chat history');
    const userId = UserService.getUserId();
    const response = await fetch(`http://localhost:8000/api/chat/history?userId=${encodeURIComponent(userId)}`, {
      method: 'GET',
      credentials: "include",
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch chat history');
    }

    const data: ConversationHistory = await response.json();
    return data.conversations;
  } catch (error) {
    console.error('Error fetching chat history:', error);
    return [];
  }
}

export function createNewConversation(): string {
  return uuidv4();
}

export async function apiUpdateConversationName(conversationId: string, name: string): Promise<void> {
  try {
    const response = await fetch(`http://localhost:8000/api/chat/json/${conversationId}/rename`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ name })
    });
    
    if (!response.ok) {
      console.error('Failed to update conversation name');
    }
  } catch (error) {
    console.error('Error updating conversation name:', error);
  }
};

export async function getConversationHistory(conversationId: string): Promise<Message[]> {
  try {
    const response = await fetch(`http://localhost:8000/api/chat/history/${conversationId}`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch conversation history');
    }

    // The server returns the messages array directly, not a Conversation object
    const messages = await response.json();
    
    // Debugging log to verify the structure
    console.log('Received message data:', messages);
    
    // Return the array directly since that's what the server sends
    return Array.isArray(messages) ? messages : [];
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
 * 
 * @returns A promise that resolves when the API call is complete
 * 
 * @throws Error if the network request fails or server returns an error status
 * 
 * @example
 * ```typescript
 * const message = {
 *   id: '123',
 *   content: 'Create a login form',
 *   sender: 'user',
 *   timestamp: new Date().toISOString()
 * };
 * 
 * sendChatMessage(
 *   message,
 *   (chatResponse) => console.log('Chat response:', chatResponse),
 *   (prototypeResponse) => console.log('Prototype response:', prototypeResponse)
 * );
 * ```
 */
export async function sendChatMessage(
    message: Message,
    onChatResponse: ChatCallback,
    onPrototypeResponse: PrototypeCallback
): Promise<void> {
    try {
        if (!message.conversationId) {
            message.conversationId = createNewConversation();
        }
        
        const messagePayload: MessagePayload = {
            userID: UserService.getUserId(),
            time: message.timestamp,
            prompt: message.content,
            conversationId: message.conversationId
        };

        const response = await fetch("http://localhost:8000/api/chat/json", {
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
        throw error;
    }
}