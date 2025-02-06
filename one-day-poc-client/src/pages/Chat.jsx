import React, {useEffect, useRef} from "react";
import { useLocation } from "react-router-dom";
import ChatMessage from '../hooks/Chat';

function Chat() {
    const {
        message,
        setMessage,
        sentMessages,
        handleSend,
    } = ChatMessage();

    const location = useLocation();
    const initialMessage = location.state?.initialMessage;

    /*
        * This useEffect hook is used to send the initial message to the chat taken from the landing page
     */
    useEffect(() => {
        if (initialMessage) {
            setMessage(initialMessage);
            handleSend();
        }
    }, []);

    const recentMessageRef = useRef(null);

    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            handleSend();
        }
    };

    useEffect(() => {
        if (recentMessageRef.current) {
            recentMessageRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [sentMessages]);

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

                {/* Message List */}
                <div
                    style={{
                        flex: 1,
                        overflowY: "auto",
                        padding: "10px",
                        display: "flex",
                        flexDirection: "column",
                    }}
                >
                    {/*List of messages*/}
                    {sentMessages.map((msg, index) => (
                        msg.role === "User" ? (
                            <div
                                key={index}
                                style={{
                                    padding: "10px",
                                    backgroundColor: "#f1f1f1",
                                    borderRadius: "5px",
                                    marginBottom: "10px",
                                    alignSelf: "flex-end",
                                    wordWrap: "break-word",
                                    maxWidth: "70%",
                                }}
                            >
                                <p>{msg.timestamp}</p>
                                <strong>User:</strong> {msg.content}
                            </div>
                        ) : (
                            <div
                                key={index}
                                style={{
                                    padding: "10px",
                                    backgroundColor: "#f1f1f1",
                                    borderRadius: "5px",
                                    marginBottom: "10px",
                                    alignSelf: "flex-start",
                                    wordWrap: "break-word",
                                    maxWidth: "70%",
                                }}
                            >
                                <p>{msg.timestamp}</p>
                                <strong>LLM:</strong> {msg.content}
                            </div>
                        )
                    ))}

                    {/* Dummy div to enable auto-scrolling */}
                    <div ref={recentMessageRef} />
                </div>

                {/* Input and Send Button */}
                <div
                    style={{
                        display: "flex",
                        padding: "10px",
                        borderTop: "1px solid #ccc",
                    }}
                >
                    <input
                        type="text"
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        placeholder="How can we help you today?"
                        onKeyDown={handleKeyDown}
                        style={{
                            flex: 1,
                            padding: "10px",
                            border: "1px solid #ccc",
                            borderRadius: "5px",
                            marginRight: "10px",
                        }}
                    />
                    <button
                        onClick={handleSend}
                        style={{
                            padding: "10px 20px",
                            backgroundColor: "#007bff",
                            color: "white",
                            border: "none",
                            borderRadius: "5px",
                            cursor: "pointer",
                        }}
                    >
                        Send
                    </button>
                </div>
            </div>
            {/*Prototype side*/}
            <div style={{ flex: 3,borderStyle: 'solid',}}>
                <h3> Prototype</h3>
            </div>
        </div>
    );
}

export default Chat;