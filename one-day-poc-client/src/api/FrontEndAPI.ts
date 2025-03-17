import { Message, ChatResponse, PrototypeResponse, ServerResponse, MessagePayload } from "../types/Types";

type ChatCallback = (chatResponse: ChatResponse) => void;
type PrototypeCallback = (prototypeResponse: PrototypeResponse) => void;

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