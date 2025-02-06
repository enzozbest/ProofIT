import React, { useState } from "react";
import { Paperclip, SendHorizontal } from "lucide-react";
import { useNavigate } from "react-router-dom";

const InputBox = () => {
    const [message, setMessage] = useState("");
    const navigate = useNavigate();

    /*
     * navigate to the chat page when the user has written a prompt and sent, store it in a state
     */
    const handleNavigateToChat = () => {
        if (message.trim()) {
            navigate('/chat', { state: { initialMessage: message } });
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && message.trim()) {
            handleNavigateToChat();
        }
    };

    return (
        <div className="flex items-center border rounded-lg p-4 w-full max-w-2xl my-10 h-14">
            <input
                type="text"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Tell us what we can do for you."
                className="flex-1 bg-transparent px-4 py-2 outline-none text-lg"
            />
            <button className="p-3 bg-white flex items-center justify-center w-12 h-12 border rounded-lg">
                <Paperclip size={22} />
            </button>
            <button
                onClick={handleNavigateToChat}
                className="p-3 bg-white flex items-center justify-center w-12 h-12 border rounded-lg ml-2 hover:bg-gray-50 active:bg-gray-100 transition-colors"
            >
                <SendHorizontal size={22} />
            </button>
        </div>
    );
};

export default InputBox;