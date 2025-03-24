package chat.routes

import chat.BaseAuthenticationServer
import chat.GET
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class GetHistoryTest : BaseAuthenticationServer() {
    @Test
    fun `Test unauthorized access`() =
        testApplication {
            setupTestApplication()
            val response = client.get(GET)
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test authorized access`() =
        testApplication {
            setupTestApplication()
            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("conversations"))
        }

    @Test
    fun `Test invalid token`() =
        testApplication {
            setupTestApplication()
            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer invalid-token")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
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
            application {
                routing {
                    chatRoutes()
                }
            }

            val response = client.get(GET)
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
        }

    @Test
    fun `Test chatRoutes extension function on Route object`() =
        testApplication {
            application {
                routing {
                    val route = this
                    route.chatRoutes()
                }
            }

            val response = client.get(GET)
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
        }


    @Test
    fun `Test ConversationHistory data class`() =
        testApplication {
            val conversation1 = Conversation(
                id = "123",
                name = "Chat 1",
                lastModified = "2024-03-24T12:00:00Z",
                messageCount = 5,
                userId = "user_123"
            )

            val conversation2 = Conversation(
                id = "456",
                name = "Chat 2",
                lastModified = "2024-03-25T14:00:00Z",
                messageCount = 10,
                userId = "user_456"
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
            val conversation = Conversation(
                id = "123",
                name = "Test Conversation",
                lastModified = "2024-03-24T12:00:00Z",
                messageCount = 10,
                userId = "user_123"
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
            val message = MessageDto(
                id = "123",
                conversationId="1",
                timestamp = "2024-03-24T12:00:00Z",
                content = "Hello",
                senderId = "1"
            )

            assertEquals("123", message.id)
            assertEquals("1", message.conversationId)
            assertEquals("2024-03-24T12:00:00Z", message.timestamp)
            assertEquals("Hello", message.content)
            assertEquals("1", message.senderId)
        }


    @Test
    fun `Test PrototypeDto data class`() =
        testApplication {
            val prototype = PrototypeDto(
                files = "New prototype",
            )

            assertEquals("New prototype", prototype.files)
        }


}
