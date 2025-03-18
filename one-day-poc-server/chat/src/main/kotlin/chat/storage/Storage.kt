package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository
import database.tables.chats.ChatMessage
import database.tables.chats.Conversation
import org.jetbrains.exposed.sql.Database

object ChatStorageFactory {
    private val repository by lazy {
        val db = DatabaseManager.init()
        ChatRepository(db)
    }
    
    fun getChatRepository(): ChatRepository = repository
}

suspend fun storeMessage(message: ChatMessage): Boolean {
    return ChatStorageFactory.getChatRepository().saveMessage(message)
}

suspend fun getMessageHistory(conversationId: String, limit: Int = 50, offset: Int = 0): List<ChatMessage> {
    return ChatStorageFactory.getChatRepository().getMessagesByConversation(conversationId, limit, offset)
}

suspend fun getConversationHistory(userId: String): List<Conversation> {
    return ChatStorageFactory.getChatRepository().getConversationsByUser(userId)
}

suspend fun updateConversationName(conversationId: String, name: String): Boolean {
    return ChatStorageFactory.getChatRepository().updateConversationName(conversationId, name)
}
