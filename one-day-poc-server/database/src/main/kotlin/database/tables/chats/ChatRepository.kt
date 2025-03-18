package database.tables.chats

import database.core.DatabaseManager
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID
import chat.storage.ChatMessage
import chat.storage.ChatStorage
import chat.storage.Conversation

class ChatRepository(private val db: Database){
    companion object {
        private val IO_DISPATCHER = Dispatchers.IO
    }

    suspend fun saveMessage(message: ChatMessage): Boolean {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                val conversationId = UUID.fromString(message.conversationId)
                val conversation = ConversationEntity.findById(conversationId) ?: 
                    ConversationEntity.new(conversationId) {
                        name = "New Conversation"
                        lastModified = message.timestamp
                    }
                
                conversation.lastModified = message.timestamp
                
                val messageId = UUID.fromString(message.id)
                ChatMessageEntity.new(messageId) {
                    this.conversation = conversation
                    this.senderId = message.senderId
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
    
    suspend fun getMessagesByUser(userId: String, limit: Int, offset: Int): List<ChatMessage> {
        return try {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                ChatMessageEntity.find { ChatMessageTable.senderId eq userId }
                    .orderBy(ChatMessageTable.timestamp to SortOrder.ASC)
                    .limit(limit, offset.toLong())
                    .map { it.toChatMessage() }
            }
        } catch (e: Exception) {
            println("Error retrieving user messages: ${e.message}")
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
}