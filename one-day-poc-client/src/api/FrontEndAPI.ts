import { Message, ChatResponse, PrototypeResponse, FileTree, ServerResponse, MessagePayload } from "../types/Types";

import hardcoded from './hardcoded.json';

const testFiles = hardcoded;  // hardcoded for now TODO: change to dynamic

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
        
        if (serverResponse.chat) {
            onChatResponse(serverResponse.chat);
        }
        
        // if (serverResponse.prototype) {
            // onPrototypeResponse(serverResponse.prototype);
        if(true){
            onPrototypeResponse({ files: testFiles });
        } else {
            // For testing only: remove this in production
            console.log('No prototype in response, using test files');
            onPrototypeResponse({ files: testFiles });
        }


    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}