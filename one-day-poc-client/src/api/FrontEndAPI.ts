import { Message, ChatResponse, PrototypeResponse, ServerResponse, MessagePayload } from "../types/Types";

type ChatCallback = (chatResponse: ChatResponse) => void;
type PrototypeCallback = (prototypeResponse: PrototypeResponse) => void;

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
        const messagePayload: MessagePayload = {
            userID: "user123",
            time: message.timestamp,
            prompt: message.content
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