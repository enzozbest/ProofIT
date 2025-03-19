package database.tables.chats

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.Instant
import java.util.UUID
import database.tables.chats.ChatMessage
import database.tables.chats.Conversation

class ConversationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ConversationEntity>(ConversationTable)
    
    var name by ConversationTable.name
    var lastModified by ConversationTable.lastModified
    var userId by ConversationTable.userId
    
    val messages by ChatMessageEntity referrersOn ChatMessageTable.conversationId
    
    fun toConversation(messageCount: Int): Conversation {
        return Conversation(
            id = id.value.toString(),
            name = name,
            lastModified = lastModified.toString(),
            messageCount = messageCount,
            userId = userId
        )
    }
}

class ChatMessageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ChatMessageEntity>(ChatMessageTable)
    
    var conversation by ConversationEntity referencedOn ChatMessageTable.conversationId
    var isFromLLM by ChatMessageTable.isFromLLM
    var content by ChatMessageTable.content
    var timestamp by ChatMessageTable.timestamp
    
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id.value.toString(),
            conversationId = conversation.id.value.toString(),
            senderId = if (isFromLLM) "LLM" else "user",
            content = content,
            timestamp = timestamp
        )
    }
}