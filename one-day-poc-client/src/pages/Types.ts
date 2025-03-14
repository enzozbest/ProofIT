import { Dispatch, SetStateAction } from 'react';

interface WebContainerFile {
    file: {
        contents: string;
    };
}

interface FileTree {
    [key: string]: WebContainerFile;
}

export interface ChatScreenProps {
    showPrototype: boolean;
    setPrototype: Dispatch<SetStateAction<boolean>>;
    setPrototypeFiles: Dispatch<SetStateAction<FileTree>>;
}