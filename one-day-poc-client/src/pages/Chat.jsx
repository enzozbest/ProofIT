import React,{ useState, useRef, useEffect } from "react";
import { ChatBox } from "../components/chat-box";
import { MessageBox } from "../components/messages-box";


function Chat() {
    const [sentMessage, setSentMessage] = useState([]);
    
    return (
        <div className="relative h-full flex flex-col">
                
                <MessageBox sentMessage={sentMessage}/>
                <ChatBox setSentMessage={setSentMessage} setError={setErrorMessage}/>
            
        </div>

    );
}

export default Chat;