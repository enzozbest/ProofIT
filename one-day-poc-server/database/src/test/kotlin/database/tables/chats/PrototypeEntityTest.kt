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

class PrototypeEntityTest {
    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        MockEnvironment.postgresContainer.start()
        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(ConversationTable, ChatMessageTable, PrototypeTable)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(PrototypeTable, ChatMessageTable, ConversationTable)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()
    }
    
    @Test
    fun `test creating PrototypeEntity and converting to model`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val conversation = ConversationEntity.new(conversationId) {
                name = "Test Conversation"
                lastModified = Instant.now()
                userId = "test-user"
            }
            
            val messageId = UUID.randomUUID()
            val message = ChatMessageEntity.new(messageId) {
                this.conversation = conversation
                isFromLLM = false
                content = "Test message"
                timestamp = Instant.now()
            }
            
            val prototypeId = UUID.randomUUID()
            val testJson = """{"files":[{"name":"test.js","content":"console.log('hello')"}]}"""
            val testTimestamp = Instant.now()
            
            val prototype = PrototypeEntity.new(prototypeId) {
                this.message = message
                filesJson = testJson
                version = 2
                isSelected = true
                timestamp = testTimestamp
            }
            
            assertEquals(message.id, prototype.message.id)
            assertEquals(testJson, prototype.filesJson)
            assertEquals(2, prototype.version)
            assertTrue(prototype.isSelected)
            assertEquals(testTimestamp, prototype.timestamp)
            
            val model = prototype.toPrototype()
            assertEquals(prototypeId.toString(), model.id)
            assertEquals(messageId.toString(), model.messageId)
            assertEquals(testJson, model.filesJson)
            assertEquals(2, model.version)
            assertTrue(model.isSelected)
            assertEquals(testTimestamp, model.timestamp)
        }
    }
    
    @Test
    fun `test updating PrototypeEntity`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val conversation = ConversationEntity.new(conversationId) {
                name = "Test Conversation"
                lastModified = Instant.now()
                userId = "test-user"
            }
            
            val messageId = UUID.randomUUID()
            val message = ChatMessageEntity.new(messageId) {
                this.conversation = conversation
                isFromLLM = false
                content = "Test message"
                timestamp = Instant.now()
            }
            
            val prototypeId = UUID.randomUUID()
            val prototype = PrototypeEntity.new(prototypeId) {
                this.message = message
                filesJson = """{"files":[]}"""
                version = 1
                isSelected = true
                timestamp = Instant.now()
            }
            
            val updatedJson = """{"files":[{"name":"updated.js"}]}"""
            val updatedTimestamp = Instant.now().plusSeconds(3600)
            
            prototype.filesJson = updatedJson
            prototype.version = 2
            prototype.isSelected = false
            prototype.timestamp = updatedTimestamp
            
            val retrievedPrototype = PrototypeEntity.findById(prototypeId)
            assertNotNull(retrievedPrototype)
            assertEquals(updatedJson, retrievedPrototype.filesJson)
            assertEquals(2, retrievedPrototype.version)
            assertFalse(retrievedPrototype.isSelected)
            assertEquals(updatedTimestamp, retrievedPrototype.timestamp)
        }
    }
    
    @Test
    fun `test deleting PrototypeEntity`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val conversation = ConversationEntity.new(conversationId) {
                name = "Test Conversation"
                lastModified = Instant.now()
                userId = "test-user"
            }
            
            val messageId = UUID.randomUUID()
            val message = ChatMessageEntity.new(messageId) {
                this.conversation = conversation
                isFromLLM = false
                content = "Test message"
                timestamp = Instant.now()
            }
            
            val prototypeId = UUID.randomUUID()
            val prototype = PrototypeEntity.new(prototypeId) {
                this.message = message
                filesJson = """{"files":[]}"""
                version = 1
                isSelected = true
                timestamp = Instant.now()
            }
            
            assertNotNull(PrototypeEntity.findById(prototypeId))
            
            prototype.delete()
            
            assertNull(PrototypeEntity.findById(prototypeId))
        }
    }
}