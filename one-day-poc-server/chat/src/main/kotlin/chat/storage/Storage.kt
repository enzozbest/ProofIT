package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository

object ChatStorageFactory {
    private val storage by lazy {
        val db = DatabaseManager.init()
        ChatRepository(db)
    }
    
    fun getStorage(): ChatStorage = storage
}

suspend fun storeMessage(message: ChatMessage): Boolean {
    return ChatStorageFactory.getStorage().saveMessage(message)
}

suspend fun getMessageHistory(conversationId: String, limit: Int = 50): List<ChatMessage> {
    return ChatStorageFactory.getStorage().getMessagesByConversation(conversationId, limit)
}