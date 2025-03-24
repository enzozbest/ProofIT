package database

import database.core.DatabaseManager
import database.helpers.MockEnvironment
import database.tables.chats.ChatMessage
import database.tables.chats.ChatMessageTable
import database.tables.chats.ChatRepository
import database.tables.chats.ConversationTable
import database.tables.chats.Prototype
import database.tables.chats.PrototypeTable
import database.tables.templates.Template
import database.tables.templates.TemplateRepository
import database.tables.templates.Templates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.environment.EnvironmentLoader
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineCoverageTest {
    private lateinit var db: Database
    private lateinit var chatRepository: ChatRepository
    private lateinit var templateRepository: TemplateRepository
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    // Invalid database for testing failure paths
    private val invalidDb = Database.connect(
        url = "jdbc:postgresql://localhost:5432/nonexistentdb",
        driver = "org.postgresql.Driver",
        user = "invalid",
        password = "invalid"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        MockEnvironment.postgresContainer.start()

        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(ConversationTable)
            SchemaUtils.create(ChatMessageTable)
            SchemaUtils.create(PrototypeTable)
            SchemaUtils.create(Templates)
        }
        chatRepository = ChatRepository(db)
        templateRepository = TemplateRepository(db)
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(PrototypeTable)
            SchemaUtils.drop(ChatMessageTable)
            SchemaUtils.drop(ConversationTable)
            SchemaUtils.drop(Templates)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()

        Dispatchers.resetMain()
    }

    @Test
    fun `Test savePrototype with StandardTestDispatcher`() = runTest(testDispatcher) {
        // First create a message
        val conversationId = UUID.randomUUID().toString()
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = "user1",
            content = "Test message",
            timestamp = Instant.now()
        )

        val messageSaveResult = chatRepository.saveMessage(message)
        testScheduler.advanceUntilIdle()
        assertTrue(messageSaveResult)

        // Now create a prototype for this message
        val prototype = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            filesJson = """{"files":[{"name":"test.js","content":"console.log('hello')"}]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )

        val result = chatRepository.savePrototype(prototype)
        testScheduler.advanceUntilIdle()

        assertTrue(result)
    }

    @Test
    fun `Test getPrototype with StandardTestDispatcher`() = runTest(testDispatcher) {
        // First create a message
        val conversationId = UUID.randomUUID().toString()
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = "user1",
            content = "Test message",
            timestamp = Instant.now()
        )

        chatRepository.saveMessage(message)
        testScheduler.advanceUntilIdle()

        // Create a prototype
        val prototypeId = UUID.randomUUID().toString()
        val filesJson = """{"files":[{"name":"test.js","content":"console.log('hello')"}]}"""
        val prototype = Prototype(
            id = prototypeId,
            messageId = messageId,
            filesJson = filesJson,
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )

        chatRepository.savePrototype(prototype)
        testScheduler.advanceUntilIdle()

        // Retrieve the prototype by message ID
        val prototypes = chatRepository.getPrototypesByMessageId(messageId)
        testScheduler.advanceUntilIdle()

        assertEquals(1, prototypes.size)
        assertEquals(filesJson, prototypes[0].filesJson)
        assertEquals(messageId, prototypes[0].messageId)
    }

    @Test
    fun `Test get all prototypes in conversation with StandardTestDispatcher`() = runTest(testDispatcher) {
        // Create a conversation with two messages
        val conversationId = UUID.randomUUID().toString()

        val message1Id = UUID.randomUUID().toString()
        val message1 = ChatMessage(
            id = message1Id,
            conversationId = conversationId,
            senderId = "user1",
            content = "First message",
            timestamp = Instant.now()
        )

        val message2Id = UUID.randomUUID().toString()
        val message2 = ChatMessage(
            id = message2Id,
            conversationId = conversationId,
            senderId = "user1",
            content = "Second message",
            timestamp = Instant.now()
        )

        chatRepository.saveMessage(message1)
        chatRepository.saveMessage(message2)
        testScheduler.advanceUntilIdle()

        // Create prototypes for each message
        val prototype1 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = message1Id,
            filesJson = """{"files":[{"name":"first.js"}]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )

        val prototype2 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = message2Id,
            filesJson = """{"files":[{"name":"second.js"}]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )

        chatRepository.savePrototype(prototype1)
        chatRepository.savePrototype(prototype2)
        testScheduler.advanceUntilIdle()

        // Get all prototypes in the conversation
        val prototypes = chatRepository.getAllPrototypesInConversation(conversationId)
        testScheduler.advanceUntilIdle()

        assertEquals(2, prototypes.size)
    }

    @Test
    fun `Test saveTemplateToDB with StandardTestDispatcher`() = runTest(testDispatcher) {
        val template = Template(
            id = "test-template-id",
            fileURI = "test-file-uri"
        )

        val result = templateRepository.saveTemplateToDB(template)
        testScheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `Test getTemplateFromDB with StandardTestDispatcher`() = runTest(testDispatcher) {
        val template = Template(
            id = "test-template-id",
            fileURI = "test-file-uri"
        )

        val saveResult = templateRepository.saveTemplateToDB(template)
        testScheduler.advanceUntilIdle()
        assertTrue(saveResult.isSuccess)

        val result = templateRepository.getTemplateFromDB("test-template-id")
        testScheduler.advanceUntilIdle()

        assertNotNull(result)
        assertEquals("test-file-uri", result.fileURI)
    }

    @Test
    fun `Test error handling when saving prototype`() = runTest(testDispatcher) {
        val invalidRepository = ChatRepository(invalidDb)

        val prototype = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = UUID.randomUUID().toString(), // Invalid message ID
            filesJson = """{"files":[]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )

        val result = invalidRepository.savePrototype(prototype)
        testScheduler.advanceUntilIdle()

        assertFalse(result)
    }

    @Test
    fun `Test error handling when getting prototype by messageId`() = runTest(testDispatcher) {
        val invalidRepository = ChatRepository(invalidDb)

        val result = invalidRepository.getPrototypesByMessageId("invalid-id")
        testScheduler.advanceUntilIdle()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Test saveTemplateToDB failure path with StandardTestDispatcher`() = runTest(testDispatcher) {
        val invalidRepository = TemplateRepository(invalidDb)

        val template = Template(
            id = "test-template-id",
            fileURI = "test-file-uri"
        )

        val result = invalidRepository.saveTemplateToDB(template)
        testScheduler.advanceUntilIdle()

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    @Test
    fun `Test getTemplateFromDB failure path with StandardTestDispatcher`() = runTest(testDispatcher) {
        val invalidRepository = TemplateRepository(invalidDb)

        val result = invalidRepository.getTemplateFromDB("test-template-id")
        testScheduler.advanceUntilIdle()

        assertNull(result)
    }
}
