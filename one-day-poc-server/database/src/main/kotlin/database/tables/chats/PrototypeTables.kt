package database.tables.chats

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object PrototypeTable : UUIDTable("prototypes") {
    val messageId = reference("message_id", ChatMessageTable, onDelete = ReferenceOption.CASCADE)
    val filesJson = text("files_json")
    val version = integer("version")
    val isSelected = bool("is_selected")
    val timestamp = timestamp("timestamp")
}