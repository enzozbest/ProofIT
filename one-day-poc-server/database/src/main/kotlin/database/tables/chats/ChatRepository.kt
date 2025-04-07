package database.tables.chats

import database.core.DatabaseManager
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

class ChatRepository(private val db: Database){
    companion object {
        private val IO_DISPATCHER = Dispatchers.IO
    }

    suspend fun saveMessage(message: ChatMessage): Boolean {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val conversationId = if (message.conversationId.isNullOrBlank()) {
                    UUID.randomUUID()
                } else {
                    try {
                        UUID.fromString(message.conversationId)
                    } catch (e: Exception) {
                        UUID.randomUUID()
                    }
                }
                
                val conversation = ConversationEntity.findById(conversationId) ?: 
                    ConversationEntity.new(conversationId) {
                        name = "New Conversation"
                        lastModified = message.timestamp
                        userId = if (message.senderId != "LLM") message.senderId else "user"
                    }
                
                conversation.lastModified = message.timestamp
                
                val messageId = UUID.fromString(message.id)
                ChatMessageEntity.new(messageId) {
                    this.conversation = conversation
                    this.isFromLLM = message.senderId == "LLM"
                    this.content = message.content
                    this.timestamp = message.timestamp
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getMessageById(messageId: String): ChatMessage? {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val id = UUID.fromString(messageId)
                ChatMessageEntity.findById(id)?.toChatMessage()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getMessagesByConversation(
        conversationId: String, 
        limit: Int, 
        offset: Int
    ): List<ChatMessage> {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val id = UUID.fromString(conversationId)
                ChatMessageEntity.find { ChatMessageTable.conversationId eq id }
                    .orderBy(ChatMessageTable.timestamp to SortOrder.ASC)
                    .limit(limit, offset.toLong())
                    .map { it.toChatMessage() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun deleteMessage(messageId: String): Boolean {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val id = UUID.fromString(messageId)
                ChatMessageEntity.findById(id)?.delete()
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateConversationName(conversationId: String, name: String): Boolean {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val id = UUID.fromString(conversationId)
                val conversation = ConversationEntity.findById(id)
                if (conversation != null) {
                    conversation.name = name
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getConversationMessageCount(conversationId: String): Int {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val id = UUID.fromString(conversationId)
                ChatMessageEntity.find { ChatMessageTable.conversationId eq id }.count().toInt()
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getConversationsByUser(userId: String): List<Conversation> {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                ConversationEntity.find {
                    ConversationTable.userId eq userId
                }.map { entity ->
                    val messageCount = ChatMessageEntity.find {
                        ChatMessageTable.conversationId eq entity.id
                    }.count().toInt()
                    
                    entity.toConversation(messageCount)
                }.sortedByDescending { it.lastModified }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun savePrototype(prototype: Prototype): Boolean {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val messageId = UUID.fromString(prototype.messageId)
                val message = ChatMessageEntity.findById(messageId) 
                    ?: throw IllegalArgumentException("Message not found")
                
                val prototypeId = UUID.fromString(prototype.id)
                PrototypeEntity.new(prototypeId) {
                    this.message = message
                    this.filesJson = prototype.filesJson
                    this.version = prototype.version
                    this.isSelected = prototype.isSelected
                    this.timestamp = prototype.timestamp
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPrototypesByMessageId(messageId: String): List<Prototype> {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val id = UUID.fromString(messageId)
                PrototypeEntity.find { 
                    PrototypeTable.messageId eq id 
                }.map { it.toPrototype() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllPrototypesInConversation(conversationId: String): List<Prototype> {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val convId = UUID.fromString(conversationId)
                
                val messageIds = ChatMessageEntity.find {
                    ChatMessageTable.conversationId eq convId
                }.map { it.id }
                
                if (messageIds.isEmpty()) {
                    emptyList()
                } else {
                    PrototypeEntity.find {
                        PrototypeTable.messageId inList messageIds
                    }.map { it.toPrototype() }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSelectedPrototypeForMessage(conversationId: String, messageId: String): Prototype? {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val msgId = UUID.fromString(messageId)
                val convId = UUID.fromString(conversationId)
                
                val message = ChatMessageEntity.find {
                    (ChatMessageTable.id eq msgId) and
                    (ChatMessageTable.conversationId eq convId)
                }.firstOrNull() ?: return@newSuspendedTransaction null
                
                PrototypeEntity.find { 
                    (PrototypeTable.messageId eq msgId) and 
                    (PrototypeTable.isSelected eq true)
                }.firstOrNull()?.toPrototype()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPreviousPrototype(conversationId: String) : Prototype? {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val convId = UUID.fromString(conversationId)

                val messageIds = ChatMessageEntity.find {
                    ChatMessageTable.conversationId eq convId
                }.map { it.id }

                if (messageIds.isEmpty()) {
                    null
                } else {
                    PrototypeEntity.find {
                        (PrototypeTable.messageId inList messageIds) and
                        (PrototypeTable.isSelected eq true)
                    }.sortedByDescending { it.timestamp }
                    .firstOrNull()
                    ?.toPrototype()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteConversation(conversationId: String): Boolean {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val id = UUID.fromString(conversationId)
                val conversation = ConversationEntity.findById(id)
                if (conversation != null) {
                    conversation.delete()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}