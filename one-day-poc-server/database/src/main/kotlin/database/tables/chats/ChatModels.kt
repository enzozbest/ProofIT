package database.tables.chats

import java.time.Instant
import java.util.UUID

/**
 * Data class representing a chat message.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val senderId: String,
    val content: String,
    val timestamp: Instant = Instant.now()
)

/**
 * Data class representing a conversation summary.
 */
data class Conversation(
    val id: String,
    val name: String,
    val lastModified: String,
    val messageCount: Int,
    val userId: String
)