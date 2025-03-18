import React, { createContext, useState, useContext, useEffect } from 'react';
import { Conversation } from '../types/Types';
import { fetchChatHistory, createNewConversation } from '../api/FrontEndAPI';

interface ConversationContextType {
  conversations: Conversation[];
  activeConversationId: string | null;
  setActiveConversationId: (id: string) => void;
  createConversation: () => string;
  refreshConversations: () => Promise<void>;
  isLoading: boolean;
}

const ConversationContext = createContext<ConversationContextType | undefined>(undefined);

export const ConversationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const refreshConversations = async () => {
    setIsLoading(true);
    const chatHistory = await fetchChatHistory();
    setConversations(chatHistory);
    setIsLoading(false);
  };

  const createConversation = () => {
    const newId = createNewConversation();
    const newConversation: Conversation = {
      id: newId,
      name: `New Chat ${new Date().toLocaleString()}`,
      lastModified: new Date().toISOString(),
      messageCount: 0
    };
    
    setConversations([...conversations, newConversation]);
    setActiveConversationId(newId);
    return newId;
  };

  // Load conversations when user logs in
  useEffect(() => {
    refreshConversations();
  }, []);

  return (
    <ConversationContext.Provider
      value={{
        conversations,
        activeConversationId,
        setActiveConversationId,
        createConversation,
        refreshConversations,
        isLoading
      }}
    >
      {children}
    </ConversationContext.Provider>
  );
};

export const useConversation = () => {
  const context = useContext(ConversationContext);
  if (context === undefined) {
    throw new Error('useConversation must be used within a ConversationProvider');
  }
  return context;
};