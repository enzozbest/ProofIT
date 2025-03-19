import React, { createContext, useState, useContext, useEffect } from 'react';
import { Conversation } from '../types/Types';
import { fetchChatHistory, createNewConversation, apiUpdateConversationName } from '../api/FrontEndAPI';
import { useAuth } from '@/contexts/AuthContext';

interface ConversationContextType {
  conversations: Conversation[];
  activeConversationId: string | null;
  setActiveConversationId: (id: string) => void;
  createConversation: () => string;
  refreshConversations: () => Promise<void>;
  updateConversationName: (id: string, name: string) => Promise<void>;
  isLoading: boolean;
}

const ConversationContext = createContext<ConversationContextType | undefined>(undefined);

export const ConversationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
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

  const updateConversationName = async (id: string, name: string) => {
    setConversations(conversations.map(conv => 
      conv.id === id ? { ...conv, name } : conv
    ));
    
    try {
      await apiUpdateConversationName(id, name);
    } catch (error) {
      console.error('Error updating conversation name:', error);
    }
  };

  // Refresh when authentication status changes
  useEffect(() => {
    if (isAuthenticated) {
      console.log("User authenticated, fetching conversations");
      refreshConversations();
    } else {
      // Clear conversations when logged out
      setConversations([]);
      setActiveConversationId(null);
    }
  }, [isAuthenticated]); // Depend on isAuthenticated instead of user

  return (
    <ConversationContext.Provider
      value={{
        conversations,
        activeConversationId,
        setActiveConversationId,
        createConversation,
        refreshConversations,
        updateConversationName,
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