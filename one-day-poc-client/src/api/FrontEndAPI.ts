import { Message, ChatResponse, PrototypeResponse, FileTree, ServerResponse } from "./Types";

type ChatCallback = (chatResponse: ChatResponse) => void;
type PrototypeCallback = (prototypeResponse: PrototypeResponse) => void;

export async function getPrototypeFiles(prototypeId: string | number): Promise<FileTree> {
    const response = await fetch(`/api/prototypes/${prototypeId}/files`);
    if (!response.ok) {
        throw new Error('Failed to fetch prototype files');
    }
    const files = await response.json();
    
    const fileTree: FileTree = {};
    for (const [fileName, content] of Object.entries(files)) {
        fileTree[fileName] = {
            file: {
                contents: content as string
            }
        };
    }
    
    return fileTree;
}

export async function sendChatMessage(
    message: Message,
    onChatResponse: ChatCallback,
    onPrototypeResponse: PrototypeCallback
): Promise<void> {
    try {
        const response = await fetch("http://localhost:8000/api/chat/json", {
            method: 'POST',
            credentials: "include",
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(message)
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