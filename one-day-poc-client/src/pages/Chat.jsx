import React,{ useState, useRef, useEffect } from "react";
import { ChatBox } from "@/components/chat-box.jsx";
import { MessageBox } from "@/components/messages-box.jsx";


function Chat() {
    const [sentMessage, setSentMessage] = useState([]);
    
    return (
        <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            backgroundColor:'white',
            width:'100vw',
            height:'100vh',
            color:'black'}}
        >
            {/* Chat Side */}
            <div style={{
                flex:2,
                borderStyle: 'solid',
                display: "flex",
                flexDirection: "column",
                maxHeight: "100vh",
                overflow: "hidden"
            }}>
                <MessageBox sentMessage={sentMessage}/>
                <ChatBox setSentMessage={setSentMessage}/>
            </div>
            {/*Prototype side*/}
            <div style={{ flex: 3,borderStyle: 'solid',}}>
                <h3> Prototype</h3>
            </div>
        </div>

    );
}

export default Chat;