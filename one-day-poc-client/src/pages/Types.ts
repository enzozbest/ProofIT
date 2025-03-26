import { Dispatch, SetStateAction } from 'react';

interface WebContainerFile {
  file: {
    contents: string;
  };
}

interface FileTree {
  [key: string]: WebContainerFile;
}

/**
 * Props interface for the ChatScreen component
 *
 * Defines the required properties that must be passed to the ChatScreen component:
 * - showPrototype: Controls whether the prototype preview is visible
 * - setPrototype: Function to update the prototype visibility state
 * - setPrototypeFiles: Function to update the prototype file contents
 */
export interface ChatScreenProps {
  showPrototype: boolean;
  setPrototype: Dispatch<SetStateAction<boolean>>;
  setPrototypeFiles: Dispatch<SetStateAction<FileTree>>;
}
