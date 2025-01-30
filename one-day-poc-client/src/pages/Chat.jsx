import React,{ useState, useRef, useEffect } from "react";


function Chat() {
    const [message, setMessage] = useState("");
    const [sentMessage, setSentMessage] = useState([]);
    const recentMessageRef = useRef(null);
    const [llmResponse, setLlmResponse] = useState("");

    const postMessage = async () => {
        try {
            var response = await fetch("http://localhost:8000/api/chat/send", {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(message)
            });
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const data = await response.text();
            console.log(data);
            setLlmResponse(data);
        } catch (error) {
            console.error('Error', error);
        }
    }

    const handleSend = async () => {
        const currentTime = new Date().toLocaleString();
        setSentMessage((prevMessages) => [...prevMessages, ["User", message, currentTime]]);
        {/**In reality, the response would be the ACTUAL llm response*/}
        await postMessage();
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

    // Update sentMessage when llmResponse changes
    useEffect(() => {
        if (llmResponse) {
            const currentTime = new Date().toLocaleString();
            setSentMessage((prevMessages) => [
                ...prevMessages,
                ["LLM", llmResponse, currentTime]
            ]);
        }
    }, [llmResponse]);

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