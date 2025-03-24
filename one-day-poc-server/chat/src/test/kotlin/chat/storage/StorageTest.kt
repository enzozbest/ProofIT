package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository
import database.tables.chats.ChatMessage
import database.tables.chats.Conversation
import database.tables.chats.Prototype
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageTest {
    private val mockRepository = mockk<ChatRepository>()

    @BeforeEach
    fun setUp() {
        // Mock the ChatStorageFactory object
        mockkObject(ChatStorageFactory)
        every { ChatStorageFactory.getChatRepository() } returns mockRepository
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test storeMessage success`() = runBlocking {
        val message = ChatMessage(
            conversationId = "test-conversation",
            senderId = "test-sender",
            content = "test-content"
        )

        coEvery { mockRepository.saveMessage(message) } returns true

        val result = storeMessage(message)

        coVerify(exactly = 1) { mockRepository.saveMessage(message) }
        assertTrue(result)
    }

    @Test
    fun `test storeMessage failure`() = runBlocking {
        val message = ChatMessage(
            conversationId = "test-conversation",
            senderId = "test-sender",
            content = "test-content"
        )

        coEvery { mockRepository.saveMessage(message) } throws RuntimeException("Test exception")

        val result = storeMessage(message)

        coVerify(exactly = 1) { mockRepository.saveMessage(message) }
        assertFalse(result)
    }

    @Test
    fun `test getMessageHistory success`() = runBlocking {
        val conversationId = "test-conversation"
        val limit = 10
        val offset = 0
        val expectedMessages = listOf(
            ChatMessage(
                conversationId = conversationId,
                senderId = "test-sender",
                content = "test-content-1"
            ),
            ChatMessage(
                conversationId = conversationId,
                senderId = "test-sender",
                content = "test-content-2"
            )
        )

        coEvery { mockRepository.getMessagesByConversation(conversationId, limit, offset) } returns expectedMessages

        val result = getMessageHistory(conversationId, limit, offset)

        coVerify(exactly = 1) { mockRepository.getMessagesByConversation(conversationId, limit, offset) }
        assertEquals(expectedMessages, result)
    }

    @Test
    fun `test getMessageHistory failure`() = runBlocking {
        val conversationId = "test-conversation"
        val limit = 10
        val offset = 0

        coEvery { mockRepository.getMessagesByConversation(conversationId, limit, offset) } throws RuntimeException("Test exception")

        val result = getMessageHistory(conversationId, limit, offset)

        coVerify(exactly = 1) { mockRepository.getMessagesByConversation(conversationId, limit, offset) }
        assertEquals(emptyList(), result)
    }

    @Test
    fun `test getConversationHistory success`() = runBlocking {
        val userId = "test-user"
        val expectedConversations = listOf(
            Conversation(
                id = "test-conversation-1",
                name = "Test Conversation 1",
                userId = userId,
                lastModified = "2023-01-01",
                messageCount = 5
            ),
            Conversation(
                id = "test-conversation-2",
                name = "Test Conversation 2",
                userId = userId,
                lastModified = "2023-01-02",
                messageCount = 10
            )
        )

        coEvery { mockRepository.getConversationsByUser(userId) } returns expectedConversations

        val result = getConversationHistory(userId)

        coVerify(exactly = 1) { mockRepository.getConversationsByUser(userId) }
        assertEquals(expectedConversations, result)
    }

    @Test
    fun `test getConversationHistory failure`() = runBlocking {
        val userId = "test-user"

        coEvery { mockRepository.getConversationsByUser(userId) } throws RuntimeException("Test exception")

        val result = getConversationHistory(userId)

        coVerify(exactly = 1) { mockRepository.getConversationsByUser(userId) }
        assertEquals(emptyList(), result)
    }

    @Test
    fun `test updateConversationName success`() = runBlocking {
        val conversationId = "test-conversation"
        val name = "New Name"

        coEvery { mockRepository.updateConversationName(conversationId, name) } returns true

        val result = updateConversationName(conversationId, name)

        coVerify(exactly = 1) { mockRepository.updateConversationName(conversationId, name) }
        assertTrue(result)
    }

    @Test
    fun `test updateConversationName failure`() = runBlocking {
        val conversationId = "test-conversation"
        val name = "New Name"

        coEvery { mockRepository.updateConversationName(conversationId, name) } throws RuntimeException("Test exception")

        val result = updateConversationName(conversationId, name)

        coVerify(exactly = 1) { mockRepository.updateConversationName(conversationId, name) }
        assertFalse(result)
    }

    @Test
    fun `test storePrototype success`() = runBlocking {
        val prototype = Prototype(
            messageId = "test-message",
            filesJson = "{}",
            version = 1,
            isSelected = true
        )

        // For a Unit-returning suspend function, we need to use a different approach
        coEvery { mockRepository.savePrototype(any()) } returns true

        val result = storePrototype(prototype)

        coVerify(exactly = 1) { mockRepository.savePrototype(any()) }
        assertTrue(result)
    }

    @Test
    fun `test storePrototype failure`() = runBlocking {
        val prototype = Prototype(
            messageId = "test-message",
            filesJson = "{}",
            version = 1,
            isSelected = true
        )

        coEvery { mockRepository.savePrototype(any()) } throws RuntimeException("Test exception")

        val result = storePrototype(prototype)

        coVerify(exactly = 1) { mockRepository.savePrototype(any()) }
        assertFalse(result)
    }

    @Test
    fun `test retrievePrototype success`() = runBlocking {
        val conversationId = "test-conversation"
        val messageId = "test-message"
        val expectedPrototype = Prototype(
            messageId = messageId,
            filesJson = "{}",
            version = 1,
            isSelected = true
        )

        coEvery { mockRepository.getSelectedPrototypeForMessage(conversationId, messageId) } returns expectedPrototype

        val result = retrievePrototype(conversationId, messageId)

        coVerify(exactly = 1) { mockRepository.getSelectedPrototypeForMessage(conversationId, messageId) }
        assertEquals(expectedPrototype, result)
    }

    @Test
    fun `test retrievePrototype failure`() = runBlocking {
        val conversationId = "test-conversation"
        val messageId = "test-message"

        coEvery { mockRepository.getSelectedPrototypeForMessage(conversationId, messageId) } throws RuntimeException("Test exception")

        val result = retrievePrototype(conversationId, messageId)

        coVerify(exactly = 1) { mockRepository.getSelectedPrototypeForMessage(conversationId, messageId) }
        assertNull(result)
    }
}
