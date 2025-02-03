import React, { useState, useEffect, useRef } from "react";

export function ChatBox({ setSentMessage }) {
    const [message, setMessage] = useState("");
    const [llmResponse, setLlmResponse] = useState("");
    const recentMessageRef = useRef(null);

    const postMessage = async () => {
        try {
            const response = await fetch("http://localhost:8000/api/chat/send", {
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
            setLlmResponse(data);
        } catch (error) {
            console.error('Error', error);
        }
    };

    const handleSend = async () => {
        const currentTime = new Date().toLocaleString();
        // Ensure setSentMessage is correctly used to update the state
        setSentMessage((prevMessages) => [...prevMessages, ["User", message, currentTime]]);
        await postMessage();
        setMessage("");
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            handleSend();
        }
    };


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
    );
}
