export type MessageRole = 'User' | 'LLM';

export interface Message {
    role: MessageRole;
    content: string;
    timestamp: string;
}

export interface MessagePayload {
    userID: string;
    time: string;
    prompt: string;
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