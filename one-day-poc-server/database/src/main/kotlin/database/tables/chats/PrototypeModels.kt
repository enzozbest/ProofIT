package database.tables.chats

import java.time.Instant
import java.util.UUID

data class Prototype(
    val id: String = UUID.randomUUID().toString(),
    val messageId: String,
    val filesJson: String,
    val version: Int = 1,
    val isSelected: Boolean = true,
    val timestamp: Instant = Instant.now()
)