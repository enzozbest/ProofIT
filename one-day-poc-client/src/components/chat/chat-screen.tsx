import * as React from "react";
import { useState, useEffect } from "react";
import { ChatBox } from "./chat-box";
import { MessageBox } from "./messages-box";
import  ChatMessage  from "@/hooks/Chat";
import { ChatScreenProps } from "../../pages/Types";

import {toast} from 'sonner'

const ChatScreen: React.FC<ChatScreenProps> = ({ showPrototype, setPrototype }) =>{

    const {
        message,
        setMessage,
        sentMessages,
        handleSend,
        errorMessage,
        setErrorMessage,
    } = ChatMessage({setPrototype});


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
