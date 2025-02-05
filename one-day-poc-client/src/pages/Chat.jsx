import React,{ useState, useRef, useEffect } from "react";
import { ChatBox, CHAT_ERROR } from "../components/chat-box";
import { MessageBox } from "../components/messages-box";
import { MutedOverlay } from "../components/ui/overlay"
import{
    Frown
} from 'lucide-react'



function Chat() {
    const [sentMessage, setSentMessage] = useState([]);
    const [errorMessage, setErrorMessage] = useState("");

    
    return (
        <div className="relative h-full flex flex-col">
                {errorMessage && (
                <MutedOverlay isVisible={!!errorMessage} onClose={() => setErrorMessage("")}>
                    <p className="text-red-600">{errorMessage}</p>
                    <Frown />
                </MutedOverlay>
                )}

                <MessageBox sentMessage={sentMessage}/>
                <ChatBox setSentMessage={setSentMessage} setError={setErrorMessage}/>
            
        </div>

    );
}

export default Chat;