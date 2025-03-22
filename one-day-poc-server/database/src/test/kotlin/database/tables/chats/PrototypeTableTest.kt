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

class PrototypeTableTest {
    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        MockEnvironment.postgresContainer.start()
        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(ChatMessageTable, ConversationTable)
            SchemaUtils.create(PrototypeTable)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(PrototypeTable)
            SchemaUtils.drop(ChatMessageTable, ConversationTable)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()
    }
    
    @Test
    fun `verify PrototypeTable schema`() {
        transaction(db) {
            val columns = PrototypeTable.columns
            
            assertTrue(columns.any { it.name == "id" })
            assertTrue(columns.any { it.name == "message_id" })
            assertTrue(columns.any { it.name == "files_json" })
            assertTrue(columns.any { it.name == "version" })
            assertTrue(columns.any { it.name == "is_selected" })
            assertTrue(columns.any { it.name == "timestamp" })
            
            val messageIdColumn = columns.first { it.name == "message_id" }
            assertTrue(messageIdColumn.columnType.sqlType().contains("UUID"))
            
            val filesJsonColumn = columns.first { it.name == "files_json" }
            assertTrue(filesJsonColumn.columnType.sqlType().contains("TEXT"))
            
            val versionColumn = columns.first { it.name == "version" }
            assertTrue(versionColumn.columnType.sqlType().contains("INT"))
            
            val isSelectedColumn = columns.first { it.name == "is_selected" }
            assertTrue(isSelectedColumn.columnType.sqlType().contains("BOOL"))
            
            val timestampColumn = columns.first { it.name == "timestamp" }
            assertTrue(timestampColumn.columnType.sqlType().contains("TIMESTAMP"))
        }
    }
    
    @Test
    fun `verify foreign key constraint`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            ConversationTable.insert {
                it[id] = conversationId
                it[name] = "Test Conversation"
                it[lastModified] = Instant.now()
                it[userId] = "test-user"
            }
            
            val messageId = UUID.randomUUID()
            ChatMessageTable.insert {
                it[id] = messageId
                it[this.conversationId] = conversationId
                it[isFromLLM] = false
                it[content] = "Test message"
                it[timestamp] = Instant.now()
            }
            
            val prototypeId = UUID.randomUUID()
            PrototypeTable.insert {
                it[id] = prototypeId
                it[this.messageId] = messageId
                it[filesJson] = """{"files":[]}"""
                it[version] = 1
                it[isSelected] = true
                it[timestamp] = Instant.now()
            }
            
            val count = PrototypeTable.select { PrototypeTable.id eq prototypeId }.count()
            assertEquals(1, count)
            
            val invalidMessageId = UUID.randomUUID()
            assertFails {
                PrototypeTable.insert {
                    it[id] = UUID.randomUUID()
                    it[this.messageId] = invalidMessageId
                    it[filesJson] = """{"files":[]}"""
                    it[version] = 1
                    it[isSelected] = true
                    it[timestamp] = Instant.now()
                }
            }
        }
    }
    
    @Test
    fun `verify cascade delete`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            ConversationTable.insert {
                it[id] = conversationId
                it[name] = "Test Conversation"
                it[lastModified] = Instant.now()
                it[userId] = "test-user"
            }
            
            val messageId = UUID.randomUUID()
            ChatMessageTable.insert {
                it[id] = messageId
                it[this.conversationId] = conversationId
                it[isFromLLM] = false
                it[content] = "Test message"
                it[timestamp] = Instant.now()
            }
            
            val prototypeId = UUID.randomUUID()
            PrototypeTable.insert {
                it[id] = prototypeId
                it[this.messageId] = messageId
                it[filesJson] = """{"files":[]}"""
                it[version] = 1
                it[isSelected] = true
                it[timestamp] = Instant.now()
            }
            
            val countBefore = PrototypeTable.select { PrototypeTable.id eq prototypeId }.count()
            assertEquals(1, countBefore)
            
            ChatMessageTable.deleteWhere { ChatMessageTable.id eq messageId }
            
            val countAfter = PrototypeTable.select { PrototypeTable.id eq prototypeId }.count()
            assertEquals(0, countAfter)
        }
    }
}
