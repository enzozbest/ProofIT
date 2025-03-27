package chat.routes

import chat.BaseAuthenticationServer
import chat.GET
import chat.storage.getConversationHistory
import chat.storage.getMessageHistory
import chat.storage.retrievePrototype
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import database.tables.chats.ChatMessage as DbChatMessage
import database.tables.chats.Conversation as DbConversation
import database.tables.chats.Prototype as DbPrototype

class GetHistoryTest : BaseAuthenticationServer() {
    @BeforeEach
    fun setUp() {
        mockkStatic(::getConversationHistory)
        mockkStatic(::getMessageHistory)
        mockkStatic(::retrievePrototype)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Test authorized access with successful conversation retrieval`() =
        testApplication {
            setupTestApplication()

            val userId = "user"
            val mockConversations =
                listOf(
                    DbConversation(
                        id = "conv1",
                        name = "Conversation 1",
                        userId = userId,
                        lastModified = "2023-01-01",
                        messageCount = 5,
                    ),
                    DbConversation(
                        id = "conv2",
                        name = "Conversation 2",
                        userId = userId,
                        lastModified = "2023-01-02",
                        messageCount = 10,
                    ),
                )

            coEvery { getConversationHistory(userId) } returns mockConversations

            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("conversations"))
            assertTrue(responseBody.contains("conv1"))
            assertTrue(responseBody.contains("Conversation 1"))
            assertTrue(responseBody.contains("conv2"))
            assertTrue(responseBody.contains("Conversation 2"))

            coVerify(exactly = 1) { getConversationHistory(userId) }
        }

    @Test
    fun `Test authorized access with custom userId parameter`() =
        testApplication {
            setupTestApplication()

            val userId = "custom-user"
            val mockConversations =
                listOf(
                    DbConversation(
                        id = "conv1",
                        name = "Custom Conversation",
                        userId = userId,
                        lastModified = "2023-01-01",
                        messageCount = 5,
                    ),
                )

            coEvery { getConversationHistory(userId) } returns mockConversations

            val response =
                client.get("$GET?userId=$userId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("Custom Conversation"))

            coVerify(exactly = 1) { getConversationHistory(userId) }
        }

    @Test
    fun `Test authorized access with error in conversation retrieval`() =
        testApplication {
            setupTestApplication()

            val userId = "user"
            val errorMessage = "Database connection error"

            coEvery { getConversationHistory(userId) } throws Exception(errorMessage)

            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("Error: $errorMessage"))

            coVerify(exactly = 1) { getConversationHistory(userId) }
        }

    @Test
    fun `Test respondText function directly`() =
        testApplication {
            application {
                routing {
                    get(GET) {
                        call.respondText("Hello, world!")
                    }
                }
            }

            val response = client.get(GET)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello, world!", response.bodyAsText())
        }

    @Test
    fun `Test chatRoutes function directly`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test chatRoutes extension function on Route object`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test ConversationHistory data class`() =
        testApplication {
            val conversation1 =
                Conversation(
                    id = "123",
                    name = "Chat 1",
                    lastModified = "2024-03-24T12:00:00Z",
                    messageCount = 5,
                    userId = "user_123",
                )

            val conversation2 =
                Conversation(
                    id = "456",
                    name = "Chat 2",
                    lastModified = "2024-03-25T14:00:00Z",
                    messageCount = 10,
                    userId = "user_456",
                )

            val conversationHistory = ConversationHistory(listOf(conversation1, conversation2))

            assertEquals(2, conversationHistory.conversations.size)
            assertEquals("123", conversationHistory.conversations[0].id)
            assertEquals("Chat 1", conversationHistory.conversations[0].name)
            assertEquals("user_123", conversationHistory.conversations[0].userId)
        }

    @Test
    fun `Test Conversation data class`() =
        testApplication {
            val conversation =
                Conversation(
                    id = "123",
                    name = "Test Conversation",
                    lastModified = "2024-03-24T12:00:00Z",
                    messageCount = 10,
                    userId = "user_123",
                )

            assertEquals("123", conversation.id)
            assertEquals("Test Conversation", conversation.name)
            assertEquals("2024-03-24T12:00:00Z", conversation.lastModified)
            assertEquals(10, conversation.messageCount)
            assertEquals("user_123", conversation.userId)
        }

    @Test
    fun `Test MessageDto data class`() =
        testApplication {
            val message =
                MessageDto(
                    id = "123",
                    conversationId = "1",
                    timestamp = "2024-03-24T12:00:00Z",
                    content = "Hello",
                    senderId = "1",
                )

            assertEquals("123", message.id)
            assertEquals("1", message.conversationId)
            assertEquals("2024-03-24T12:00:00Z", message.timestamp)
            assertEquals("Hello", message.content)
            assertEquals("1", message.senderId)
        }

    @Test
    fun `Test get messages for conversation with successful retrieval`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 50
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg1",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Hello",
                        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
                    ),
                    DbChatMessage(
                        id = "msg2",
                        conversationId = conversationId,
                        senderId = "user2",
                        content = "Hi there",
                        timestamp = Instant.parse("2023-01-01T12:01:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, 0) } returns mockMessages

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Hello"))
            assertTrue(responseBody.contains("msg2"))
            assertTrue(responseBody.contains("Hi there"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, 0) }
        }

    @Test
    fun `Test get messages for conversation with custom limit`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 10
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg6",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 6",
                        timestamp = Instant.parse("2023-01-01T12:05:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=$limit") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg6"))
            assertTrue(responseBody.contains("Message 6"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit) }
        }

    @Test
    fun `Test get messages for conversation with missing conversationId`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get("$GET/") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get messages for conversation with error in message retrieval`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 50
            val errorMessage = "Database connection error"

            coEvery { getMessageHistory(conversationId, limit, 0) } throws Exception(errorMessage)

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("Error: $errorMessage"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, 0) }
        }

    @Test
    fun `Test PrototypeDto data class`() =
        testApplication {
            val prototype =
                PrototypeDto(
                    files = "New prototype",
                )

            assertEquals("New prototype", prototype.files)
        }

    @Test
    fun `Test get prototype for message with successful retrieval`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val messageId = "msg1"
            val mockPrototype =
                DbPrototype(
                    messageId = messageId,
                    filesJson = """{"file1": "content1", "file2": "content2"}""",
                    version = 1,
                    isSelected = true,
                )

            coEvery { retrievePrototype(conversationId, messageId) } returns mockPrototype

            val response =
                client.get("$GET/$conversationId/$messageId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("file1"))
            assertTrue(responseBody.contains("content1"))
            assertTrue(responseBody.contains("file2"))
            assertTrue(responseBody.contains("content2"))

            coVerify(exactly = 1) { retrievePrototype(conversationId, messageId) }
        }

    @Test
    fun `Test get prototype for message with null prototype`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val messageId = "msg1"

            coEvery { retrievePrototype(conversationId, messageId) } returns null

            val response =
                client.get("$GET/$conversationId/$messageId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.NotFound, response.status)

            coVerify(exactly = 1) { retrievePrototype(conversationId, messageId) }
        }

    @Test
    fun `Test get conversations endpoint`() =
        testApplication {
            setupTestApplication()

            val userId = "user"
            coEvery { getConversationHistory(userId) } returns emptyList()

            val response =
                client.get("$GET") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("conversations"))

            coVerify(exactly = 1) { getConversationHistory(userId) }
        }

    @Test
    fun `Test get prototype for message with null messageId parameter`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test get prototype for message with error in prototype retrieval`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val messageId = "msg1"
            val errorMessage = "Database connection error"

            coEvery { retrievePrototype(conversationId, messageId) } throws Exception(errorMessage)

            val response =
                client.get("$GET/$conversationId/$messageId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("Error: $errorMessage"))

            coVerify(exactly = 1) { retrievePrototype(conversationId, messageId) }
        }

    @Test
    fun `Test get messages for conversation with missing conversationId parameter`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get("$GET/") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get prototype with missing conversationId parameter`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get("$GET//msg1") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get prototype with missing messageId parameter`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get("$GET/conv1/") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get conversation with empty conversationId parameter`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get("$GET/") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get prototype with empty conversationId parameter`() =
        testApplication {
            setupTestApplication()

            val response =
                client.get("$GET//msg1") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
            val responseBody = response.bodyAsText()
            assertEquals("Empty conversation ID", responseBody)
        }

    @Test
    fun `Test serializable annotations`() {
        val conversation = Conversation("1", "Test", "2023-01-01", 5, "user1")
        val history = ConversationHistory(listOf(conversation))
        val message = MessageDto("1", "conv1", "user1", "Hello", "2023-01-01")
        val prototype = PrototypeDto("files")

        assertEquals("1", conversation.id)
        assertEquals(listOf(conversation), history.conversations)
        assertEquals("1", message.id)
        assertEquals("files", prototype.files)

        val json =
            kotlinx.serialization.json.Json
                .encodeToString(Conversation.serializer(), conversation)
        assertTrue(json.contains("\"id\":\"1\""))
        assertTrue(json.contains("\"name\":\"Test\""))

        val jsonHistory =
            kotlinx.serialization.json.Json
                .encodeToString(ConversationHistory.serializer(), history)
        assertTrue(jsonHistory.contains("\"conversations\""))

        val jsonMessage =
            kotlinx.serialization.json.Json
                .encodeToString(MessageDto.serializer(), message)
        assertTrue(jsonMessage.contains("\"id\":\"1\""))

        val jsonPrototype =
            kotlinx.serialization.json.Json
                .encodeToString(PrototypeDto.serializer(), prototype)
        assertTrue(jsonPrototype.contains("\"files\":\"files\""))
    }

    @Test
    fun `Test get messages for conversation with custom limit and offset`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 25
            val offset = 10
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg11",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 11",
                        timestamp = Instant.parse("2023-01-01T12:10:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=$limit&offset=$offset") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg11"))
            assertTrue(responseBody.contains("Message 11"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with invalid limit parameter`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 50 
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg1",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 1",
                        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, 0) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=invalid") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, 0) }
        }

    @Test
    fun `Test get messages for conversation with invalid offset parameter`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 50
            val offset = 0
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg1",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 1",
                        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?offset=invalid") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with all query parameters`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 25
            val offset = 10
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg11",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 11",
                        timestamp = Instant.parse("2023-01-01T12:10:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=$limit&offset=$offset") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg11"))
            assertTrue(responseBody.contains("Message 11"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with null limit parameter`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 50 
            val offset = 0 
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg1",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 1",
                        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with null offset parameter`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 30 
            val offset = 0 
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg1",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 1",
                        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=$limit") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with null limit and offset parameters`() =
        testApplication {
            setupTestApplication()

            val conversationId = "conv1"
            val limit = 50 
            val offset = 0 
            val mockMessages =
                listOf(
                    DbChatMessage(
                        id = "msg1",
                        conversationId = conversationId,
                        senderId = "user1",
                        content = "Message 1",
                        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
                    ),
                )

            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }
}
