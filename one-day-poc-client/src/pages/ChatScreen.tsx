import * as React from "react";
import {useState, useEffect} from "react";
import { ChatBox} from "@/components/chat-box";
import { MessageBox } from "@/components/messages-box";
import { MutedOverlay } from "@/components/ui/overlay";
import { Frown, TableRowsSplit } from 'lucide-react'
import  ChatMessage from "@/hooks/Chat";
import { ChatScreenProps } from "./Types";

import {toast} from 'sonner'


const ChatScreen: React.FC<ChatScreenProps> = ({ showPrototype, prototypeId, setPrototype, setPrototypeId }) =>{

    const {
        message,
        setMessage,
        sentMessages,
        handleSend,
        errorMessage,
        setErrorMessage,
    } = ChatMessage({setPrototype, setPrototypeId, prototypeId});


    useEffect(() => {
        if (errorMessage) {
            toast.error(errorMessage, {
                onDismiss: () => setErrorMessage(""), 
                onAutoClose: () => setErrorMessage(""),
                closeButton: true
            });
        }
    }, [errorMessage]);
    return (
        <div className="relative h-full flex flex-col">

            <MessageBox sentMessages={sentMessages}/>
            <ChatBox
                setMessage={setMessage}
                message={message}
                handleSend={handleSend}
                setError={setErrorMessage}
            />

        </div>

    );
}

export default ChatScreen;
