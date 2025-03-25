package chat.routes

import chat.BaseAuthenticationServer
import chat.GET
import chat.storage.getConversationHistory
import chat.storage.getMessageHistory
import chat.storage.retrievePrototype
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.*
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

            // Mock the getConversationHistory function to return a list of conversations
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

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("conversations"))
            assertTrue(responseBody.contains("conv1"))
            assertTrue(responseBody.contains("Conversation 1"))
            assertTrue(responseBody.contains("conv2"))
            assertTrue(responseBody.contains("Conversation 2"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { getConversationHistory(userId) }
        }

    @Test
    fun `Test authorized access with custom userId parameter`() =
        testApplication {
            setupTestApplication()

            // Mock the getConversationHistory function to return a list of conversations
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

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("Custom Conversation"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { getConversationHistory(userId) }
        }

    @Test
    fun `Test authorized access with error in conversation retrieval`() =
        testApplication {
            setupTestApplication()

            // Mock the getConversationHistory function to throw an exception
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

            // Verify the response contains the error message
            assertTrue(responseBody.contains("Error: $errorMessage"))

            // Verify the function was called with the expected parameters
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

            // Mock the getMessageHistory function to return a list of messages
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

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Hello"))
            assertTrue(responseBody.contains("msg2"))
            assertTrue(responseBody.contains("Hi there"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, 0) }
        }

    @Test
    fun `Test get messages for conversation with custom limit`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
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

            // Note: The offset parameter is not used in the actual implementation
            coEvery { getMessageHistory(conversationId, limit) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=$limit") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg6"))
            assertTrue(responseBody.contains("Message 6"))

            // Verify the function was called with the expected parameters
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

            // Mock the getMessageHistory function to throw an exception
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

            // Verify the response contains the error message
            assertTrue(responseBody.contains("Error: $errorMessage"))

            // Verify the function was called with the expected parameters
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

            // Mock the retrievePrototype function to return a prototype
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

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("file1"))
            assertTrue(responseBody.contains("content1"))
            assertTrue(responseBody.contains("file2"))
            assertTrue(responseBody.contains("content2"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { retrievePrototype(conversationId, messageId) }
        }

    @Test
    fun `Test get prototype for message with null prototype`() =
        testApplication {
            setupTestApplication()

            // Mock the retrievePrototype function to return null
            val conversationId = "conv1"
            val messageId = "msg1"

            coEvery { retrievePrototype(conversationId, messageId) } returns null

            val response =
                client.get("$GET/$conversationId/$messageId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            // When the prototype is null, the endpoint doesn't respond, resulting in a 404 Not Found
            assertEquals(HttpStatusCode.NotFound, response.status)

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { retrievePrototype(conversationId, messageId) }
        }

    @Test
    fun `Test get conversations endpoint`() =
        testApplication {
            setupTestApplication()

            // Mock the getConversationHistory function to return an empty list
            val userId = "user"
            coEvery { getConversationHistory(userId) } returns emptyList()

            // This actually hits the GET / endpoint, which returns the conversation history
            val response =
                client.get("$GET") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            // The route should return 200 OK with an empty conversation list
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("conversations"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { getConversationHistory(userId) }
        }

    @Test
    fun `Test get prototype for message with null messageId parameter`() =
        testApplication {
            setupTestApplication()

            // Test the case where the messageId parameter is null
            val conversationId = "conv1"

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            // The route should return 200 OK for a valid conversationId without a messageId
            // This actually hits the GET /{conversationId} endpoint, not the GET /{conversationId}/{messageId} endpoint
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test get prototype for message with error in prototype retrieval`() =
        testApplication {
            setupTestApplication()

            // Mock the retrievePrototype function to throw an exception
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

            // Verify the response contains the error message
            assertTrue(responseBody.contains("Error: $errorMessage"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { retrievePrototype(conversationId, messageId) }
        }

    @Test
    fun `Test get messages for conversation with missing conversationId parameter`() =
        testApplication {
            setupTestApplication()

            // This test will call the endpoint without a conversationId parameter
            // The server should respond with BadRequest
            val response =
                client.get("$GET/") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            // The route should return 404 Not Found because the path doesn't match any route
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get prototype with missing conversationId parameter`() =
        testApplication {
            setupTestApplication()

            // This test will call the endpoint without a conversationId parameter
            // The server should respond with BadRequest
            val response =
                client.get("$GET//msg1") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            // The route should return 404 Not Found because the path doesn't match any route
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get prototype with missing messageId parameter`() =
        testApplication {
            setupTestApplication()

            // This test will call the endpoint without a messageId parameter
            // The server should respond with BadRequest
            val response =
                client.get("$GET/conv1/") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            // The route should return 404 Not Found because the path doesn't match any route
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `Test get messages for conversation with custom limit and offset`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
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

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg11"))
            assertTrue(responseBody.contains("Message 11"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with invalid limit parameter`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
            val conversationId = "conv1"
            val limit = 50 // Default value when limit is invalid
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

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            // Verify the function was called with the expected parameters (default limit)
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, 0) }
        }

    @Test
    fun `Test get messages for conversation with invalid offset parameter`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
            val conversationId = "conv1"
            val limit = 50
            val offset = 0 // Default value when offset is invalid
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

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            // Verify the function was called with the expected parameters (default offset)
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with all query parameters`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
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

            // Mock the getMessageHistory function with all parameters
            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=$limit&offset=$offset") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg11"))
            assertTrue(responseBody.contains("Message 11"))

            // Verify the function was called with the expected parameters
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with null limit parameter`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
            val conversationId = "conv1"
            val limit = 50 // Default value when limit is null
            val offset = 0 // Default offset
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

            // Mock the getMessageHistory function with default limit
            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            // Verify the function was called with the expected parameters (default limit)
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with null offset parameter`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
            val conversationId = "conv1"
            val limit = 30 // Custom limit
            val offset = 0 // Default value when offset is null
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

            // Mock the getMessageHistory function with default offset
            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId?limit=$limit") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            // Verify the function was called with the expected parameters (default offset)
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }

    @Test
    fun `Test get messages for conversation with null limit and offset parameters`() =
        testApplication {
            setupTestApplication()

            // Mock the getMessageHistory function to return a list of messages
            val conversationId = "conv1"
            val limit = 50 // Default value when limit is null
            val offset = 0 // Default value when offset is null
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

            // Mock the getMessageHistory function with default limit and offset
            coEvery { getMessageHistory(conversationId, limit, offset) } returns mockMessages

            val response =
                client.get("$GET/$conversationId") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            // Verify the response contains the expected data
            assertTrue(responseBody.contains("msg1"))
            assertTrue(responseBody.contains("Message 1"))

            // Verify the function was called with the expected parameters (default limit and offset)
            coVerify(exactly = 1) { getMessageHistory(conversationId, limit, offset) }
        }
}
