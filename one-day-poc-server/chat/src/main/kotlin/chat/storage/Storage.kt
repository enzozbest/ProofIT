package chat.storage

import utils.storage.StorageService
import chat.Request
import java.time.Instant
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val senderId: String,
    val content: String,
    val timestamp: Instant = Instant.now(),
    val metadata: Map<String, Any> = emptyMap()
)

interface ChatStorage {
    fun saveMessage(message: ChatMessage): Boolean
    fun getMessageById(messageId: String): ChatMessage?
    fun getMessagesByConversation(conversationId: String, limit: Int = 50, offset: Int = 0): List<ChatMessage>
    fun getMessagesByUser(userId: String, limit: Int = 50, offset: Int = 0): List<ChatMessage>
    fun deleteMessage(messageId: String): Boolean
}

class LocalChatStorage : ChatStorage {
    private val messages = mutableMapOf<String, ChatMessage>()
    
    override fun saveMessage(message: ChatMessage): Boolean {
        messages[message.id] = message
        return true
    }
    
    override fun getMessageById(messageId: String): ChatMessage? {
        return messages[messageId]
    }
    
    override fun getMessagesByConversation(conversationId: String, limit: Int, offset: Int): List<ChatMessage> {
        return messages.values
            .filter { it.conversationId == conversationId }
            .sortedBy { it.timestamp }
            .drop(offset)
            .take(limit)
    }
    
    override fun getMessagesByUser(userId: String, limit: Int, offset: Int): List<ChatMessage> {
        return messages.values
            .filter { it.senderId == userId }
            .sortedBy { it.timestamp }
            .drop(offset)
            .take(limit)
    }
    
    override fun deleteMessage(messageId: String): Boolean {
        return messages.remove(messageId) != null
    }
}

class RemoteChatStorage : ChatStorage {
    override fun saveMessage(message: ChatMessage): Boolean {
        // Implement API calls to your remote service
        // Example: Use HTTP client to POST the message
        return true
    }
    
    override fun getMessageById(messageId: String): ChatMessage? {
        // Implement remote retrieval
        return null
    }
    
    override fun getMessagesByConversation(conversationId: String, limit: Int, offset: Int): List<ChatMessage> {
        // Implement remote query
        return emptyList()
    }
    
    override fun getMessagesByUser(userId: String, limit: Int, offset: Int): List<ChatMessage> {
        // Implement remote query
        return emptyList()
    }
    
    override fun deleteMessage(messageId: String): Boolean {
        // Implement remote deletion
        return false
    }
}

object ChatStorageFactory {
    private val localStorage = LocalChatStorage()
    private val remoteStorage = RemoteChatStorage()
    
    fun getStorage(useLocal: Boolean): ChatStorage {
        return if (useLocal) localStorage else remoteStorage
    }
}

fun storeMessage(message: ChatMessage, useLocal: Boolean): Boolean {
    return ChatStorageFactory.getStorage(useLocal).saveMessage(message)
}

fun getMessageHistory(conversationId: String, limit: Int = 50, useLocal: Boolean = true): List<ChatMessage> {
    return ChatStorageFactory.getStorage(useLocal).getMessagesByConversation(conversationId, limit)
}