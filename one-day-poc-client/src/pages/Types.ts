import { Dispatch, SetStateAction } from 'react';

export interface ChatScreenProps {
    showPrototype: boolean;
    setPrototype: Dispatch<SetStateAction<boolean>>;
}