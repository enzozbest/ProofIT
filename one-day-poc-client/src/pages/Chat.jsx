import React,{ useState, useRef, useEffect } from "react";
import { ChatBox, CHAT_ERROR } from "@/components/chat-box";
import { MessageBox } from "@/components/messages-box";
import { MutedOverlay } from "@/components/ui/overlay";
import { Frown } from 'lucide-react'
import ChatMessage from "@/hooks/Chat.js";



function Chat() {
    const [sentMessage, setSentMessage] = useState([]);
    const [errorMessage, setErrorMessage] = useState("");

    const {
        message,
        setMessage,
        sentMessages,
        handleSend,
    } = ChatMessage();

    return (
        <div className="relative h-full flex flex-col">
            {errorMessage && (
                <MutedOverlay isVisible={!!errorMessage} onClose={() => setErrorMessage("")}>
                    <p className="text-red-600">{errorMessage}</p>
                    <Frown />
                </MutedOverlay>
            )}

            <MessageBox sentMessages={sentMessages}/>
            <ChatBox setMessage={setMessage} message={message} handleSend={handleSend} setError={setErrorMessage}/>

        </div>

    );
}

export default Chat;