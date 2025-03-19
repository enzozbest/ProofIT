package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository
import database.tables.chats.ChatMessage
import database.tables.chats.Conversation
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object ChatStorageFactory {
    private val repository by lazy {
        val db = DatabaseManager.externalInit()
        checkDatabaseConnection(db)
        ChatRepository(db)
    }
    
    private fun checkDatabaseConnection(db: Database): Boolean {
        return try {
            transaction(db) {
                val result = exec("SELECT 1") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
                println("✅ Database connection successful: test query returned $result")
                result == 1
            }
        } catch (e: Exception) {
            println("❌ Database connection failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    fun getChatRepository(): ChatRepository = repository
}

suspend fun storeMessage(message: ChatMessage): Boolean {
    println("Storing message: ${message.content} from ${message.senderId} in ${message.conversationId}")
    return runCatching {
        ChatStorageFactory.getChatRepository().saveMessage(message)
    }.getOrElse { e ->
        println("Error storing message: ${e.message}")
        false
    }
}

suspend fun getMessageHistory(conversationId: String, limit: Int = 50, offset: Int = 0): List<ChatMessage> {
    return runCatching {
        ChatStorageFactory.getChatRepository().getMessagesByConversation(conversationId, limit, offset)
    }.getOrElse { e ->
        println("Error retrieving message history: ${e.message}")
        emptyList()
    }
}

suspend fun getConversationHistory(userId: String): List<Conversation> {
    return runCatching {
        ChatStorageFactory.getChatRepository().getConversationsByUser(userId)
    }.getOrElse { e ->
        println("Error retrieving conversation history: ${e.message}")
        emptyList()
    }
}

suspend fun updateConversationName(conversationId: String, name: String): Boolean {
    return runCatching {
        ChatStorageFactory.getChatRepository().updateConversationName(conversationId, name)
    }.getOrElse { e ->
        println("Error updating conversation name: ${e.message}")
        false
    }
}
