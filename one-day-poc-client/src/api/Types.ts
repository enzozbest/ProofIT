export interface WebContainerFile {
    file: {
        contents: string;
    };
}

export type MessageRole = 'User' | 'LLM';

export interface FileTree {
    [filePath: string]: {
        file?: {
            contents: string;
        };
        directory?: {
            [fileName: string]: any;
        };
    };
}

export interface Message {
    role: MessageRole;
    content: string;
    timestamp: string;
}

export interface ServerResponse {
    chat?: ChatResponse;
    prototype?: PrototypeResponse;
}

export interface ChatResponse {
    message: string;
    role: MessageRole;
    timestamp: string;
}

export interface PrototypeResponse {
    files: FileTree;
}

export interface MessagePayload {
    userID: string;
    time: string;
    prompt: string;
}
