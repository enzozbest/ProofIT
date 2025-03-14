import { Dispatch, SetStateAction } from 'react';

export type MessageRole = 'User' | 'LLM';

export interface Message {
    role: MessageRole;
    content: string;
    timestamp: string;
}

export interface WebContainerFile {
    file: {
        contents: string;
    };
}

export interface WebContainerDirectory {
    directory: {
        [fileName: string]: WebContainerFile | WebContainerDirectory;
    };
}

export type FileSystemEntry = WebContainerFile | WebContainerDirectory;

export interface FileTree {
    [path: string]: FileSystemEntry;
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

export interface ChatScreenProps {
    showPrototype: boolean;
    setPrototype: Dispatch<SetStateAction<boolean>>;
    setPrototypeFiles: Dispatch<SetStateAction<FileTree>>;
    initialMessage? : string | null;
}

export interface ChatMessageProps {
    setPrototype: (value: boolean) => void;
    setPrototypeFiles: (files: FileTree) => void;
}

export interface ChatHookReturn {
    message: string;
    setMessage: (message: string) => void;
    sentMessages: Message[];
    handleSend: (messageToSend?: string) => Promise<void>;
    llmResponse: string;
    errorMessage: string | null;
    setErrorMessage: (error:string | null) => void;
}

export interface PrototypeFrameProps {
    files: FileTree;
    width?: string;
    height?: string;
}