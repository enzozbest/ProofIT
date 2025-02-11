export interface ChatScreenProps {
    showPrototype: boolean;
    prototypeId: number;
    setPrototype: React.Dispatch<React.SetStateAction<boolean>>;
    setPrototypeId: React.Dispatch<React.SetStateAction<number>>;
}