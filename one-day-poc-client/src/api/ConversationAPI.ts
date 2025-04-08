import { Conversation, ConversationHistory } from '../types/Types';
import { v4 as uuidv4 } from 'uuid';
import UserService from '../services/UserService';
import { fetchWithAuth, API_BASE_URL } from './APIUtils';

export async function fetchChatHistory(): Promise<Conversation[]> {
  try {
    if (!UserService.getUser()) {
      console.log('Cannot fetch chat history as user is not logged in');
      return [];
    }
    console.log('Fetching chat history');
    const userId = UserService.getUserId();
    const response = await fetchWithAuth(
      `/chat/history?userId=${encodeURIComponent(userId)}`
    );

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

export async function apiUpdateConversationName(
  conversationId: string,
  name: string
): Promise<void> {
  try {
    const response = await fetchWithAuth(
      `/chat/json/${conversationId}/rename`,
      {
        method: 'POST',
        body: JSON.stringify({ name }),
      }
    );

    if (!response.ok) {
      console.error('Failed to update conversation name');
    }
  } catch (error) {
    console.error('Error updating conversation name:', error);
  }
}

export async function apiDeleteConversation(
  conversationId: string
): Promise<boolean> {
  try {
    const response = await fetchWithAuth(
      `/chat/json/${conversationId}/delete`,
      {
        method: 'POST',
      }
    );

    if (!response.ok) {
      console.error('Failed to delete conversation');
      return false;
    }
    
    return true;
  } catch (error) {
    console.error('Error deleting conversation:', error);
    return false;
  }
}