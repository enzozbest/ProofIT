/**
 * Represents a file in the WebContainer filesystem.
 */
export interface WebContainerFile {
    file: {
        contents: string;
    };
}

/**
 * Defines possible roles for message senders in a chat conversation.
 * 'User' represents messages from the human user.
 * 'LLM' represents messages from the AI model.
 */
export type MessageRole = 'User' | 'LLM';

/**
 * Represents a file system structure for the prototype.
 * Keys are file paths, values can be either files or directories.
 */
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

/**
 * Represents a single message in a chat conversation.
 */
export interface Message {
    role: MessageRole;
    content: string;
    timestamp: string;
}

/**
 * The complete response from the server, containing both chat and prototype data.
 * Either or both properties may be present.
 */
export interface ServerResponse {
    chat?: ChatResponse;
    prototype?: PrototypeResponse;
}

/**
 * Response containing a new chat message from the server.
 */
export interface ChatResponse {
    message: string;
    role: MessageRole;
    timestamp: string;
}

/**
 * Response containing prototype file structure and content.
 */
export interface PrototypeResponse {
    files: FileTree;
}

/**
 * Payload sent to the server for a new user message.
 */
export interface MessagePayload {
    userID: string;
    time: string;
    prompt: string;
}
