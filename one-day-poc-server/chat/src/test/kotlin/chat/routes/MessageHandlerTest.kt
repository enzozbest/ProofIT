package chat.routes

import chat.storage.storeMessage
import chat.storage.storePrototype
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessageHandlerTest {
    @BeforeEach
    fun setUp() {
        mockkStatic("chat.storage.StorageKt")
        coEvery { storeMessage(any()) } returns true
        coEvery { storePrototype(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Test saveMessage with valid parameters`() =
        runBlocking {
            // Given
            val conversationId = "test-conversation-id"
            val senderId = "test-sender-id"
            val content = "Test message content"

            // When
            val result = MessageHandler.saveMessage(conversationId, senderId, content)

            // Then
            assertEquals(conversationId, result.conversationId)
            assertEquals(senderId, result.senderId)
            assertEquals(content, result.content)
            assertNotNull(result.id)
            assertNotNull(result.timestamp)

            coVerify(exactly = 1) { storeMessage(any()) }
        }

    @Test
    fun `Test savePrototype with valid parameters and non-null prototypeFilesJson`() =
        runBlocking {
            // Given
            val conversationId = "test-conversation-id"
            val chatContent = "Test chat content"
            val prototypeFilesJson = """{"file1.txt": "content1"}"""

            // When
            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            // Then
            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with valid parameters and null prototypeFilesJson`() =
        runBlocking {
            // Given
            val conversationId = "test-conversation-id"
            val chatContent = "Test chat content"
            val prototypeFilesJson: String? = null

            // When
            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            // Then
            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 0) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with empty prototypeFilesJson`() =
        runBlocking {
            // Given
            val conversationId = "test-conversation-id"
            val chatContent = "Test chat content"
            val prototypeFilesJson = ""

            // When
            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            // Then
            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with empty conversationId`() =
        runBlocking {
            // Given
            val conversationId = ""
            val chatContent = "Test chat content"
            val prototypeFilesJson = """{"file1.txt": "content1"}"""

            // When
            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            // Then
            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with empty chatContent`() =
        runBlocking {
            // Given
            val conversationId = "test-conversation-id"
            val chatContent = ""
            val prototypeFilesJson = """{"file1.txt": "content1"}"""

            // When
            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            // Then
            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }
}
