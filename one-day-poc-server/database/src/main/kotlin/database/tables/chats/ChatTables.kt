package database.tables.chats

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.ReferenceOption

object ChatMessageTable : UUIDTable("chat_messages") {
    val conversationId = reference("conversation_id", ConversationTable, onDelete = ReferenceOption.CASCADE)
    val senderId = varchar("sender_id", 128)
    val content = text("content")
    val timestamp = timestamp("timestamp")
}

object ConversationTable : UUIDTable("conversations") {
    val name = varchar("name", 255)
    val lastModified = timestamp("last_modified")
}