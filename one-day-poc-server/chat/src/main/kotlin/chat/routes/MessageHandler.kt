package chat.routes

import chat.storage.storeMessage
import chat.storage.storePrototype
import database.tables.chats.ChatMessage
import database.tables.chats.Prototype

/**
 * Utility class for handling message and prototype operations.
 * 
 * This class provides methods to save messages and prototypes to the database.
 */
object MessageHandler {
    
    /**
     * Saves a message to the database.
     *
     * @param conversationId The ID of the conversation
     * @param senderId The ID of the sender
     * @param content The content of the message
     * @return The saved ChatMessage
     */
    suspend fun saveMessage(
        conversationId: String,
        senderId: String,
        content: String,
    ): ChatMessage {
        val message =
            ChatMessage(
                conversationId = conversationId,
                senderId = senderId,
                content = content,
            )
        storeMessage(message)
        return message
    }
    
    /**
     * Stores a prototype and LLM message from string content.
     * This is an adapted version that works with string extraction rather than JsonObject deserialization.
     *
     * @param conversationId The ID of the conversation
     * @param chatContent The content for the LLM message
     * @param prototypeFilesJson The JSON string for the prototype files or null if no prototype
     * @return The ID of the saved message
     */
    suspend fun savePrototype(
        conversationId: String,
        chatContent: String,
        prototypeFilesJson: String?,
    ): String {
        val savedMessage = saveMessage(conversationId, "LLM", chatContent)
        
        if (prototypeFilesJson != null) {
            val prototype =
                Prototype(
                    messageId = savedMessage.id,
                    filesJson = prototypeFilesJson,
                    version = 1,
                    isSelected = true,
                )
            storePrototype(prototype)
        }
        
        return savedMessage.id
    }
}