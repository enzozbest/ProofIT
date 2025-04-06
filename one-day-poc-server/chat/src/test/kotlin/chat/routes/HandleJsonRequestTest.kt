package chat.routes

import database.tables.chats.Prototype
import chat.Request
import chat.storage.getPreviousPrototype
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prompting.PredefinedPrototypes
import prompting.PredefinedPrototypeTemplate
import prompting.PromptingMain
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

class HandleJsonRequestTest {
    
    @BeforeEach
    fun setUp() {
        mockkObject(MessageHandler)
        mockkObject(PromptingMainProvider)
        mockkObject(PredefinedPrototypes)
        mockkObject(JsonProcessor)
        mockkStatic("chat.storage.StorageKt")
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
        resetPromptingMain()
    }

    @Test
    fun `test non-predefined flow`() = testApplication {
        val mockPromptingMain = mockk<PromptingMain>()
        setPromptingMain(mockPromptingMain)

        val request = Request(
            prompt = "Create a login page",
            userID = "user123",
            conversationId = "conv456",
            time = Instant.now().toString(),
            predefined = false
        )

        val previousPrototype = Prototype(
            messageId = "any-message-id",
            filesJson = "prev-json"
        )
        val promptResponse = "Raw JSON response from LLM"
        val processedResponse = Pair("Chat content", "Prototype JSON")

        coEvery { getPreviousPrototype(any()) } returns previousPrototype
        coEvery { MessageHandler.saveMessage(any(), any(), any()) } returns mockk()
        every { PromptingMainProvider.getInstance() } returns mockPromptingMain
        coEvery { mockPromptingMain.run(any(), any()) } returns promptResponse
        every { JsonProcessor.processRawJsonResponse(any()) } returns processedResponse
        coEvery { MessageHandler.savePrototype(any(), any(), any()) } returns "msg123"

        application {
            install(ContentNegotiation) {
                json()
            }

            install(Authentication) {
                basic("auth-test") {
                    validate { credentials ->
                        UserIdPrincipal(credentials.name)
                    }
                }
            }

            routing {
                route("/api/chat") {
                    authenticate("auth-test") {
                        post("/json") {
                            handleJsonRequest(request, call)
                        }
                    }
                }
            }
        }

        val response = client.post("/api/chat/json") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
            basicAuth("test-user", "test-password")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("\"message\": \"Chat content\""))
        assertTrue(responseBody.contains("\"files\": Prototype JSON"))

        coVerify {
            getPreviousPrototype("conv456")
            MessageHandler.saveMessage("conv456", "user123", "Create a login page")
            MessageHandler.savePrototype("conv456", "Chat content", "Prototype JSON")
        }
        coVerify { mockPromptingMain.run("Create a login page", "prev-json") }
    }

    @Test
    fun `test predefined prototype flow`() = testApplication {
        val request = Request(
            prompt = "Predefined template",
            userID = "user123",
            conversationId = "conv789",
            time = Instant.now().toString(),
            predefined = true
        )

        val predefinedResponse = PredefinedPrototypeTemplate(
            chatMessage = "Predefined chat message",
            files = "{\"predefined\":\"files\"}"
        )

        coEvery { PredefinedPrototypes.run(any()) } returns predefinedResponse
        coEvery { MessageHandler.savePrototype(any(), any(), any()) } returns "msg456"

        application {
            install(ContentNegotiation) {
                json()
            }

            install(Authentication) {
                basic("auth-test") {
                    validate { credentials ->
                        UserIdPrincipal(credentials.name)
                    }
                }
            }

            routing {
                route("/api/chat") {
                    authenticate("auth-test") {
                        post("/json") {
                            handleJsonRequest(request, call)
                        }
                    }
                }
            }
        }

        val response = client.post("/api/chat/json") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
            basicAuth("test-user", "test-password")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("\"message\": \"Predefined chat message\""))
        assertTrue(responseBody.contains("\"files\": {\"predefined\":\"files\"}"))

        coVerify {
            PredefinedPrototypes.run("Predefined template")
            MessageHandler.savePrototype("conv789", "Predefined chat message", "{\"predefined\":\"files\"}")
        }
    }

    @Test
    fun `test exception handling`() = testApplication {
        val request = Request(
            prompt = "Trigger exception",
            userID = "user123",
            conversationId = "conv999",
            time = Instant.now().toString(),
            predefined = false
        )

        coEvery { getPreviousPrototype(any()) } throws RuntimeException("Test exception")

        application {
            install(ContentNegotiation) {
                json()
            }

            install(Authentication) {
                basic("auth-test") {
                    validate { credentials ->
                        UserIdPrincipal(credentials.name)
                    }
                }
            }

            routing {
                route("/api/chat") {
                    authenticate("auth-test") {
                        post("/json") {
                            handleJsonRequest(request, call)
                        }
                    }
                }
            }
        }

        val response = client.post("/api/chat/json") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
            basicAuth("test-user", "test-password")
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Error processing request: Test exception"))

        coVerify { getPreviousPrototype("conv999") }
    }

    @Test
    fun `test blank prompt returns bad request`() = testApplication {
        // Arrange
        val request = Request(
            prompt = "",
            userID = "user123",
            conversationId = "conv456",
            time = Instant.now().toString(),
            predefined = false
        )


        application {
            install(ContentNegotiation) {
                json()
            }

            install(Authentication) {
                basic("auth-test") {
                    validate { credentials ->
                        UserIdPrincipal(credentials.name)
                    }
                }
            }

            routing {
                route("/api/chat") {
                    authenticate("auth-test") {
                        post("/json") {
                            handleJsonRequest(request, call)
                        }
                    }
                }
            }
        }

        val response = client.post("/api/chat/json") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
            basicAuth("test-user", "test-password")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertEquals("Invalid request: Empty prompt", responseBody)

        coVerify(exactly = 0) { getPreviousPrototype(any()) }
        coVerify(exactly = 0) { MessageHandler.saveMessage(any(), any(), any()) }
    }
}