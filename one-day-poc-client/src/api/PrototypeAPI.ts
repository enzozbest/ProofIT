import { FileTree } from '@/types/Types';
import { fetchWithAuth } from './APIUtils';

export async function getPrototypeForMessage(
  conversationId: string,
  messageId: string
): Promise<FileTree | null> {
  try {
    console.log('Fetching prototype with:', { conversationId, messageId });

    if (!conversationId || !messageId) {
      console.error('Missing required IDs:', { conversationId, messageId });
      throw new Error('Invalid conversation or message ID');
    }

    const response = await fetchWithAuth(
      `/chat/history/${conversationId}/${messageId}`
    );

    if (!response.ok) {
      console.error(
        'Request failed for:',
        { conversationId, messageId },
        'Status:',
        response.status
      );
      throw new Error('Failed to fetch prototype');
    }

    console.log('Successfully fetched prototype for:', {
      conversationId,
      messageId,
    });
    const data = await response.json();
    return JSON.parse(data.files);
  } catch (error) {
    console.error('Error fetching prototype:', error, 'For IDs:', {
      conversationId,
      messageId,
    });
    return null;
  }
}