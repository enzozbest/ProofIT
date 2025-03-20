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
            println("DEBUG - ChatMessage type: ${message::class.qualifiedName}")
            println("DEBUG - conversationId before transaction: '${message.conversationId}'")
            
            newSuspendedTransaction(IO_DISPATCHER, db) {
                println("DEBUG - conversationId inside transaction: '${message.conversationId}'")
                
                val conversationId = if (message.conversationId.isNullOrBlank()) {
                    println("DEBUG - Using random UUID since conversationId was empty")
                    UUID.randomUUID()
                } else {
                    try {
                        UUID.fromString(message.conversationId)
                    } catch (e: Exception) {
                        println("DEBUG - UUID parse failed: ${e.message}")
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
            println("Error saving message: ${e.message}")
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
            println("Error retrieving message: ${e.message}")
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
            println("Error retrieving conversation messages: ${e.message}")
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
            println("Error deleting message: ${e.message}")
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
            println("Error updating conversation name: ${e.message}")
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
            println("Error counting messages: ${e.message}")
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
            println("Error retrieving user conversations: ${e.message}")
            emptyList()
        }
    }
}