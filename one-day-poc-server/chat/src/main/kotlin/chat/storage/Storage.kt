package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository
import database.tables.chats.ChatMessage

object ChatStorageFactory {
    private val repository by lazy {
        val db = DatabaseManager.init()
        ChatRepository(db)
    }
    
    fun getRepository(): ChatRepository = repository
}

suspend fun storeMessage(message: ChatMessage): Boolean {
    return ChatStorageFactory.getRepository().saveMessage(message)
}

suspend fun getMessageHistory(conversationId: String, limit: Int = 50): List<ChatMessage> {
    return ChatStorageFactory.getRepository().getMessagesByConversation(conversationId, limit)
}