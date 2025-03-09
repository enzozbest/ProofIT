import { Dispatch, SetStateAction } from 'react';

interface WebContainerFile {
    file: {
        contents: string;
    };
}

interface FileTree {
    [key: string]: WebContainerFile;
}

export type MessageRole = 'User' | 'LLM';

export interface Message {
    role: MessageRole;
    content: string;
    timestamp: string;
}

export interface ChatResponse {
    message: string;
    role: MessageRole;
    timestamp: string;
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

export interface ChatMessageProps {
    setPrototype: Dispatch<SetStateAction<boolean>>;
}

export interface PrototypeFrameProps {
    files?: FileTree;
    width?: string;
    height?: string;
}