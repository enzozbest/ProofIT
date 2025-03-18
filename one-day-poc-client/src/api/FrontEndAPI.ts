import { Message, ChatResponse, PrototypeResponse, ServerResponse, MessagePayload, Conversation, ConversationHistory } from "../types/Types";
import UserService from "../services/UserService";
import { v4 as uuidv4 } from 'uuid';

type ChatCallback = (chatResponse: ChatResponse) => void;
type PrototypeCallback = (prototypeResponse: PrototypeResponse) => void;

export async function fetchChatHistory(): Promise<Conversation[]> {
  try {
    if (!UserService.getUser()) {
      return [];
    }
    
    const response = await fetch("http://localhost:8000/api/chat/history", {
      method: 'GET',
      credentials: "include",
      headers: {
        'Content-Type': 'application/json',
      }
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