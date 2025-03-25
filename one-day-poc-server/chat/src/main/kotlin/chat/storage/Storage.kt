package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository
import database.tables.chats.ChatMessage
import database.tables.chats.Conversation
import database.tables.chats.Prototype

object ChatStorageFactory {
    private val repository by lazy {
        DatabaseManager.externalInit()
        DatabaseManager.chatRepository()
    }
    
    fun getChatRepository(): ChatRepository = repository
}

suspend fun storeMessage(message: ChatMessage): Boolean {
    println("Storing message: ${message.content} from ${message.senderId} in ${message.conversationId}")
    return runCatching {
        println("Storing message: ${message.content} from ${message.senderId} in ${message.conversationId}")
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

suspend fun storePrototype(prototype: Prototype): Boolean {
    return runCatching {
        ChatStorageFactory.getChatRepository().savePrototype(prototype)
        true
    }.getOrElse { e ->
        println("Error storing prototype: ${e.message}")
        false
    }
}

suspend fun retrievePrototype(conversationId: String, messageId: String): Prototype? {
    return runCatching {
        ChatStorageFactory.getChatRepository().getSelectedPrototypeForMessage(conversationId, messageId)
    }.getOrElse { e ->
        println("Error retrieving prototype: ${e.message}")
        null
    }
}

suspend fun getPreviousPrototype(conversationId: String): Prototype? {
    return runCatching {
        ChatStorageFactory.getChatRepository().getPreviousPrototype(conversationId)
    }.getOrElse { e ->
        println("Error retrieving previous prototype: ${e.message}")
        null
    }
}
