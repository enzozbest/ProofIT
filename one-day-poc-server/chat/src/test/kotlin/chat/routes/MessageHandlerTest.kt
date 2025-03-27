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
            val conversationId = "test-conversation-id"
            val senderId = "test-sender-id"
            val content = "Test message content"

         
            val result = MessageHandler.saveMessage(conversationId, senderId, content)


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

            val conversationId = "test-conversation-id"
            val chatContent = "Test chat content"
            val prototypeFilesJson = """{"file1.txt": "content1"}"""

            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with valid parameters and null prototypeFilesJson`() =
        runBlocking {
            val conversationId = "test-conversation-id"
            val chatContent = "Test chat content"
            val prototypeFilesJson: String? = null

            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 0) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with empty prototypeFilesJson`() =
        runBlocking {
            val conversationId = "test-conversation-id"
            val chatContent = "Test chat content"
            val prototypeFilesJson = ""

            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with empty conversationId`() =
        runBlocking {
            val conversationId = ""
            val chatContent = "Test chat content"
            val prototypeFilesJson = """{"file1.txt": "content1"}"""

            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }

    @Test
    fun `Test savePrototype with empty chatContent`() =
        runBlocking {
            val conversationId = "test-conversation-id"
            val chatContent = ""
            val prototypeFilesJson = """{"file1.txt": "content1"}"""

            val result = MessageHandler.savePrototype(conversationId, chatContent, prototypeFilesJson)

            assertNotNull(result)

            coVerify(exactly = 1) { storeMessage(any()) }
            coVerify(exactly = 1) { storePrototype(any()) }
        }
}
