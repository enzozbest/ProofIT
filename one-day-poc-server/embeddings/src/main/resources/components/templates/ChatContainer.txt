import React, { useState, useRef, useEffect } from "react";

/** Import ChatMessage object and ChatMessageProps interface from ChatMessage.tsx.
    Change this import statement to import other similar objects and interfaces, and with the correct path
    if ChatMessage object and/or ChatMessageProps interface are not available in this project.
    Change the name of the object and/or interface throughout this file if they are named differently.
*/
import ChatMessage, { ChatMessageProps } from "./ChatMessage";

/** Import ChatInput component from ChatInput.tsx.
    Change this import statement to import other similar components, and with the correct path
    if ChatInput component is not available in this project.
    Change the name of the component throughout this file if they are named differently.
*/
import ChatInput from "./ChatInput";

/** Import TypingIndicator component from TypingIndicator.tsx.
    Change this import statement to import other similar components, and with the correct path
    if TypingIndicator component is not available in this project.
    Change the name of the component throughout this file if they are named differently.
*/
import TypingIndicator from "./TypingIndicator";

/** Import cn function from utils.ts.
    Change this import statement to import other similar functions, and with the correct path
    if cn function is not available in this project.
    Change the name of the function throughout this file if they are named differently.
*/
import { cn } from "@/lib/utils";

const ChatContainer: React.FC = () => {
    const [messages, setMessages] = useState<ChatMessageProps[]>([
        {
            content: "Hello! I'm your AI assistant. How can I help you today?",
            sender: "bot",
            timestamp: new Date(),
        },
    ]);
    const [isTyping, setIsTyping] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    const handleSendMessage = (content: string) => {
        const userMessage: ChatMessageProps = {
            content,
            sender: "user",
            timestamp: new Date(),
        };

        setMessages((prev) => [...prev, userMessage]);

        // typing animation
        setIsTyping(true);

        // add a delay before bot responds
        setTimeout(
            () => {
                const randomResponse =
                    aiResponses[Math.floor(Math.random() * aiResponses.length)];
                const botMessage: ChatMessageProps = {
                    content: randomResponse,
                    sender: "bot",
                    timestamp: new Date(),
                };

                setIsTyping(false);
                setMessages((prev) => [...prev, botMessage]);
            },
            1500 + Math.random() * 1500,
        );
    };

    return (
        <div className="flex flex-col h-full w-full max-w-4xl mx-auto rounded-2xl overflow-hidden chat-container glass-morphism border border-border/40">
            {/* Chat header */}
            <div className="flex items-center justify-between p-4 border-b border-border/30">
                <div className="flex items-center space-x-2">
                    <div className="w-2 h-2 rounded-full bg-primary animate-pulse"></div>
                    <h2 className="font-medium">AI Assistant</h2>
                </div>
            </div>

            {/* Messages container */}
            <div className="flex-1 overflow-y-auto p-4 bg-background/30">
                <div className="space-y-2">
                    {messages.map((msg, index) => (
                        <ChatMessage
                            key={index}
                            content={msg.content}
                            sender={msg.sender}
                            timestamp={msg.timestamp}
                            animate={index > messages.length - 2}
                        />
                    ))}

                    {isTyping && (
                        <div className="flex mt-4 ml-2 animate-fade-in">
                            <TypingIndicator />
                        </div>
                    )}

                    <div ref={messagesEndRef} />
                </div>
            </div>

            {/* Input area */}
            <div
                className={cn(
                    "p-4 border-t border-border/30 transition-all duration-300",
                    isTyping ? "opacity-70" : "opacity-100",
                )}
            >
                <ChatInput
                    onSendMessage={handleSendMessage}
                    disabled={isTyping}
                />
            </div>
        </div>
    );
};

export default ChatContainer;
