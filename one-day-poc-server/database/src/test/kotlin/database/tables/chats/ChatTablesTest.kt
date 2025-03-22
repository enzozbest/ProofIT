package database.tables.chats

import database.core.DatabaseManager
import database.helpers.MockEnvironment
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.environment.EnvironmentLoader
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.*

class ChatTablesTest {
    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        MockEnvironment.postgresContainer.start()
        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(ConversationTable, ChatMessageTable)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(ChatMessageTable, ConversationTable)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()
    }
    
    @Test
    fun `verify ConversationTable schema`() {
        transaction(db) {
            val columns = ConversationTable.columns
            
            assertTrue(columns.any { it.name == "id" })
            assertTrue(columns.any { it.name == "name" })
            assertTrue(columns.any { it.name == "last_modified" })
            assertTrue(columns.any { it.name == "user_id" })
            
            val nameColumn = columns.first { it.name == "name" }
            assertTrue(nameColumn.columnType.sqlType().contains("VARCHAR"))
            
            val lastModifiedColumn = columns.first { it.name == "last_modified" }
            assertTrue(lastModifiedColumn.columnType.sqlType().contains("TIMESTAMP"))
            
            val userIdColumn = columns.first { it.name == "user_id" }
            assertTrue(userIdColumn.columnType.sqlType().contains("VARCHAR"))
        }
    }
    
    @Test
    fun `verify ChatMessageTable schema`() {
        transaction(db) {
            val columns = ChatMessageTable.columns
            
            assertTrue(columns.any { it.name == "id" })
            assertTrue(columns.any { it.name == "conversation_id" })
            assertTrue(columns.any { it.name == "is_from_llm" })
            assertTrue(columns.any { it.name == "content" })
            assertTrue(columns.any { it.name == "timestamp" })
            
            val conversationIdColumn = columns.first { it.name == "conversation_id" }
            assertTrue(conversationIdColumn.columnType.sqlType().contains("UUID"))
            
            val isFromLLMColumn = columns.first { it.name == "is_from_llm" }
            assertTrue(isFromLLMColumn.columnType.sqlType().contains("BOOL"))
            
            val contentColumn = columns.first { it.name == "content" }
            assertTrue(contentColumn.columnType.sqlType().contains("TEXT"))
            
            val timestampColumn = columns.first { it.name == "timestamp" }
            assertTrue(timestampColumn.columnType.sqlType().contains("TIMESTAMP"))
        }
    }
    
    @Test
    fun `test CRUD operations on ConversationTable`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val now = Instant.now()
            ConversationTable.insert {
                it[id] = conversationId
                it[name] = "Test Conversation"
                it[lastModified] = now
                it[userId] = "test-user"
            }
            
            val conversation = ConversationTable.select { ConversationTable.id eq conversationId }.single()
            assertEquals(conversationId, conversation[ConversationTable.id].value)
            assertEquals("Test Conversation", conversation[ConversationTable.name])
            assertEquals(now, conversation[ConversationTable.lastModified])
            assertEquals("test-user", conversation[ConversationTable.userId])
            
            val updatedName = "Updated Conversation"
            val updatedTime = Instant.now().plusSeconds(3600)
            ConversationTable.update({ ConversationTable.id eq conversationId }) {
                it[name] = updatedName
                it[lastModified] = updatedTime
            }
            
            val updated = ConversationTable.select { ConversationTable.id eq conversationId }.single()
            assertEquals(updatedName, updated[ConversationTable.name])
            assertEquals(updatedTime, updated[ConversationTable.lastModified])
            
            ConversationTable.deleteWhere { ConversationTable.id eq conversationId }
            val count = ConversationTable.select { ConversationTable.id eq conversationId }.count()
            assertEquals(0, count)
        }
    }
    
    @Test
    fun `test cascade delete from conversation to messages`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            ConversationTable.insert {
                it[id] = conversationId
                it[name] = "Test Conversation"
                it[lastModified] = Instant.now()
                it[userId] = "test-user"
            }
            
            val messageIds = List(3) { UUID.randomUUID() }
            messageIds.forEach { messageId ->
                ChatMessageTable.insert {
                    it[id] = messageId
                    it[this.conversationId] = conversationId
                    it[isFromLLM] = false
                    it[content] = "Test message ${messageId}"
                    it[timestamp] = Instant.now()
                }
            }
            
            val messagesCount = ChatMessageTable.select { 
                ChatMessageTable.conversationId eq conversationId 
            }.count()
            assertEquals(3, messagesCount)
            
            ConversationTable.deleteWhere { ConversationTable.id eq conversationId }
            
            val remainingCount = ChatMessageTable.select { 
                ChatMessageTable.conversationId eq conversationId 
            }.count()
            assertEquals(0, remainingCount)
        }
    }
    
    @Test
    fun `test CRUD operations on ChatMessageTable`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            ConversationTable.insert {
                it[id] = conversationId
                it[name] = "Test Conversation"
                it[lastModified] = Instant.now()
                it[userId] = "test-user"
            }
            
            val messageId = UUID.randomUUID()
            val now = Instant.now()
            ChatMessageTable.insert {
                it[id] = messageId
                it[this.conversationId] = conversationId
                it[isFromLLM] = true
                it[content] = "Hello from LLM"
                it[timestamp] = now
            }
            
            val message = ChatMessageTable.select { ChatMessageTable.id eq messageId }.single()
            assertEquals(messageId, message[ChatMessageTable.id].value)
            assertEquals(conversationId, message[ChatMessageTable.conversationId].value)
            assertTrue(message[ChatMessageTable.isFromLLM])
            assertEquals("Hello from LLM", message[ChatMessageTable.content])
            assertEquals(now, message[ChatMessageTable.timestamp])
            
            val updatedContent = "Updated LLM message"
            ChatMessageTable.update({ ChatMessageTable.id eq messageId }) {
                it[content] = updatedContent
                it[isFromLLM] = false
            }
            
            val updated = ChatMessageTable.select { ChatMessageTable.id eq messageId }.single()
            assertEquals(updatedContent, updated[ChatMessageTable.content])
            assertFalse(updated[ChatMessageTable.isFromLLM])
            
            ChatMessageTable.deleteWhere { ChatMessageTable.id eq messageId }
            val count = ChatMessageTable.select { ChatMessageTable.id eq messageId }.count()
            assertEquals(0, count)
        }
    }
}