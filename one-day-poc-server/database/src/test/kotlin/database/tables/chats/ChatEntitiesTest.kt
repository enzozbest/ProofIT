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
import database.tables.chats.PrototypeTable

class ChatEntitiesTest {
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
            SchemaUtils.drop(PrototypeTable, ChatMessageTable, ConversationTable)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()
    }

    @Test
    fun `test ConversationEntity creation and conversion to model`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val now = Instant.now()
            val userId = "test-user-123"

            val conversation =
                ConversationEntity.new(conversationId) {
                    name = "Test Conversation"
                    lastModified = now
                    this.userId = userId
                }

            assertEquals("Test Conversation", conversation.name)
            assertEquals(now, conversation.lastModified)
            assertEquals(userId, conversation.userId)

            val model = conversation.toConversation(5)
            assertEquals(conversationId.toString(), model.id)
            assertEquals("Test Conversation", model.name)
            assertEquals(now.toString(), model.lastModified)
            assertEquals(5, model.messageCount)
            assertEquals(userId, model.userId)
        }
    }

    @Test
    fun `test ChatMessageEntity creation and conversion to model`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val conversation =
                ConversationEntity.new(conversationId) {
                    name = "Test Conversation"
                    lastModified = Instant.now()
                    userId = "test-user"
                }

            val messageId = UUID.randomUUID()
            val now = Instant.now()
            val content = "Hello world"

            val message =
                ChatMessageEntity.new(messageId) {
                    this.conversation = conversation
                    isFromLLM = true
                    this.content = content
                    timestamp = now
                }

            assertEquals(conversation.id, message.conversation.id)
            assertTrue(message.isFromLLM)
            assertEquals(content, message.content)
            assertEquals(now, message.timestamp)

            val model = message.toChatMessage()
            assertEquals(messageId.toString(), model.id)
            assertEquals(conversationId.toString(), model.conversationId)
            assertEquals("LLM", model.senderId) // Should be "LLM" when isFromLLM is true
            assertEquals(content, model.content)
            assertEquals(now, model.timestamp)
        }
    }

    @Test
    fun `test ChatMessageEntity with user sender`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val conversation =
                ConversationEntity.new(conversationId) {
                    name = "Test Conversation"
                    lastModified = Instant.now()
                    userId = "test-user"
                }

            val messageId = UUID.randomUUID()
            val message =
                ChatMessageEntity.new(messageId) {
                    this.conversation = conversation
                    isFromLLM = false
                    content = "User message"
                    timestamp = Instant.now()
                }

            val model = message.toChatMessage()
            assertEquals("user", model.senderId) // Should be "user" when isFromLLM is false
        }
    }

    @Test
    fun `test relationship between ConversationEntity and ChatMessageEntity`() {
        transaction(db) {
            val conversationId = UUID.randomUUID()
            val conversation =
                ConversationEntity.new(conversationId) {
                    name = "Test Conversation"
                    lastModified = Instant.now()
                    userId = "test-user"
                }

            ChatMessageEntity.new(UUID.randomUUID()) {
                this.conversation = conversation
                isFromLLM = false
                content = "Message 1"
                timestamp = Instant.now()
            }

            ChatMessageEntity.new(UUID.randomUUID()) {
                this.conversation = conversation
                isFromLLM = true
                content = "Message 2"
                timestamp = Instant.now()
            }

            val messages = conversation.messages.toList()
            assertEquals(2, messages.size)

            val contents = messages.map { it.content }.sorted()
            assertEquals(listOf("Message 1", "Message 2"), contents)
        }
    }
}
