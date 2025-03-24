/**
 * Application Type Definitions
 * 
 * This module provides centralized TypeScript type definitions used throughout the application.
 * It includes types for:
 * - Chat message structures
 * - WebContainer file system entities
 * - Server response formats
 * - Component props interfaces
 * 
 * These types ensure type safety and consistency across the application.
 */
import { Dispatch, SetStateAction } from 'react';

/**
 * Defines the possible roles in a chat conversation
 * - 'User' represents messages sent by the user
 * - 'LLM' represents responses from the AI language model
 */
export type MessageRole = 'User' | 'LLM';

/**
 * Message interface represents a single message in the chat conversation
 * 
 * @property {MessageRole} role - Indicates whether the message is from the user or AI
 * @property {string} content - The actual text content of the message
 * @property {string} timestamp - ISO timestamp indicating when the message was sent
 * @property {string} [conversationId] - Optional ID of the conversation this message belongs to
 * @property {string} [id] - Message ID assigned by the server
 */
export interface Message {
    role: MessageRole;
    content: string;
    timestamp: string;
    conversationId?: string;
    id?: string; // Added ID field, optional since frontend-created messages won't have it
}

/**
 * Represents a single file in the WebContainer virtual filesystem
 * 
 * Contains the textual contents of a file as required by the WebContainer API
 */
export interface WebContainerFile {
    file: {
        /** The text content of the file */
        contents: string;
    };
}

/**
 * Represents a directory in the WebContainer virtual filesystem
 * 
 * Contains a mapping of file/directory names to their content definitions
 */
export interface WebContainerDirectory {
    directory: {
        /** Maps filenames to their content or subdirectories */
        [fileName: string]: WebContainerFile | WebContainerDirectory;
    };
}

/**
 * A union type representing either a file or directory in the virtual filesystem
 */
export type FileSystemEntry = WebContainerFile | WebContainerDirectory;

/**
 * Maps file paths to their content definitions in the virtual filesystem
 * 
 * This is the top-level structure passed to WebContainer when mounting files
 */
export interface FileTree {
    /** Maps file paths (e.g. "index.html", "src/app.js") to file definitions */
    [path: string]: FileSystemEntry;
}

/**
 * Response format from the server, containing chat and/or prototype information
 * 
 * @property {ChatResponse} [chat] - Optional chat response with AI message
 * @property {PrototypeResponse} [prototype] - Optional prototype file definitions
 */
export interface ServerResponse {
    chat?: ChatResponse;
    prototype?: PrototypeResponse;
}

/**
 * Structure for chat responses from the server
 * 
 * @property {string} message - The text content of the AI response
 * @property {MessageRole} role - The role of the message sender (typically 'LLM')
 * @property {string} timestamp - ISO timestamp when the response was generated
 * @property {string} [messageId] - Unique ID for the message generated by the server
 */
export interface ChatResponse {
    message: string;
    role: MessageRole;
    timestamp: string;
    messageId?: string;
}

/**
 * Structure for prototype generation responses from the server
 * 
 * @property {FileTree} files - File definitions to be mounted in WebContainer
 */
export interface PrototypeResponse {
    files: FileTree;
}

/**
 * Payload structure for sending messages to the server
 * 
 * @property {string} userID - Unique identifier for the current user
 * @property {string} time - ISO timestamp of when the message was sent
 * @property {string} prompt - The actual text content of the user's message
 */
export interface MessagePayload {
    userID: string;
    time: string;
    prompt: string;
    conversationId?: string;
}

/**
 * Props interface for the ChatScreen component
 * 
 * @property {boolean} showPrototype - Controls whether the prototype preview is visible
 * @property {Function} setPrototype - Function to update the prototype visibility state
 * @property {Function} setPrototypeFiles - Function to update the prototype file contents
 * @property {string|null} [initialMessage] - Optional initial message to process automatically
 */
export interface ChatScreenProps {
    showPrototype: boolean;
    setPrototype: Dispatch<SetStateAction<boolean>>;
    setPrototypeFiles: Dispatch<SetStateAction<FileTree>>;
    initialMessage? : string | null;
}

/**
 * Props interface for the ChatMessage hook
 * 
 * @property {Function} setPrototype - Function to update the prototype visibility state
 * @property {Function} setPrototypeFiles - Function to update the prototype file contents
 */
export interface ChatMessageProps {
    setPrototype: (value: boolean) => void;
    setPrototypeFiles: (files: FileTree) => void;
}

/**
 * Return type interface for the ChatMessage hook
 * 
 * @property {string} message - Current input message text
 * @property {Function} setMessage - Function to update the input message
 * @property {Message[]} sentMessages - Array of all messages in the conversation
 * @property {Function} handleSend - Function to send the current message
 * @property {string|null} errorMessage - Any error message that occurred
 * @property {Function} setErrorMessage - Function to update the error message
 */
export interface ChatHookReturn {
    message: string;
    setMessage: (message: string) => void;
    sentMessages: Message[];
    handleSend: (messageToSend?: string) => Promise<void>;
    errorMessage: string | null;
    setErrorMessage: (error:string | null) => void;
}

/**
 * Props interface for the PrototypeFrame component
 * 
 * @property {FileTree} files - File definitions to render in the prototype
 * @property {string} [width='100%'] - Optional width for the prototype frame
 * @property {string} [height='100%'] - Optional height for the prototype frame
 */
export interface PrototypeFrameProps {
    files: FileTree;
    width?: string;
    height?: string;
}

export interface Conversation {
    id: string;
    name: string;
    lastModified: string;
    messageCount: number;
    messages: Message[];
  }
  
  export interface ConversationHistory {
    conversations: Conversation[];
  }

  export interface MessageBoxProps {
    sentMessages: Message[];
    onLoadPrototype: (files: FileTree) => void;
    onMessageClick?: (msg: Message) => void;
  }