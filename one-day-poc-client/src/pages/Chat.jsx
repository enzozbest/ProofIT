import React,{ useState, useRef, useEffect } from "react";
import PrototypeFrame from "./PrototypeFrame";


function Chat() {
    const [message, setMessage] = useState("");
    const [sentMessage, setSentMessage] = useState([]);
    const recentMessageRef = useRef(null);

    const handleSend = () => {
        var currentTime = new Date().toLocaleString();
        setSentMessage((prevMessages) => [...prevMessages, ["User",message, currentTime]]);
        {/**In reality, the response would be the llm response*/}
        var response = ["LLM","LLM RESPONSE", currentTime]
        setSentMessage((prevMessages) => [...prevMessages, response]);
        setMessage("");
    };

    // Also sends message if the user presses enter
    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            handleSend();
        }
    };

    // Scroll to the most recent message
    useEffect(() => {
        if (recentMessageRef.current) {
            recentMessageRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [sentMessage]);


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
                    {sentMessage.map((msg, index) => (
                        msg[0] === "User" ? (
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
                                <p>{msg[2]}</p>
                                <strong>User:</strong> {msg[1]}
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
                                <p>{msg[2]}</p>
                                <strong>LLM:</strong> {msg[1]}
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
                        placeholder="Type a message"
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
                <PrototypeFrame htmlContent={'<h1>Prototype Frame</h1>'} cssContent={'body { font-family: Arial, sans-serif; background-color: #f0f0f0; } h1 {color: #333 }'} jsContent={'console.log("hello from prototype");'} width={'100%'} height={'100%'} />
            </div>
        </div>

    );
}

export default Chat;