import * as React from "react";
import { useState, useEffect } from "react";
import { ChatBox } from "./chat-box";
import { MessageBox } from "./messages-box";
import  ChatMessage  from "@/hooks/Chat";
import { ChatScreenProps } from "../types/Types";

import {toast} from 'sonner'

const ChatScreen: React.FC<ChatScreenProps> = ({ showPrototype, setPrototype, setPrototypeFiles, initialMessage }) =>{

    const {
        message,
        setMessage,
        sentMessages,
        handleSend,
        errorMessage,
        setErrorMessage,
    } = ChatMessage({setPrototype, setPrototypeFiles});


    useEffect(() => {
        if (errorMessage) {
            toast.error(errorMessage, {
                onDismiss: () => setErrorMessage(""), 
                onAutoClose: () => setErrorMessage(""),
                closeButton: true
            });
        }
    }, [errorMessage]);

    useEffect(() => {
        if (initialMessage) {
            setMessage(initialMessage);
            const timer = setTimeout(() => {
                handleSend(initialMessage);
                sessionStorage.removeItem('initialMessage');
            }, 500);
            
            return () => clearTimeout(timer);
        }
    }, [initialMessage]);

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
