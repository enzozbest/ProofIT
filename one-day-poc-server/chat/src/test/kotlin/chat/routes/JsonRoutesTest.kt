package chat.routes

import chat.BaseAuthenticationServer
import chat.storage.getPreviousPrototype
import chat.storage.updateConversationName
import database.tables.chats.ChatMessage
import database.tables.chats.Prototype
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import prompting.PredefinedPrototypeTemplate
import prompting.PredefinedPrototypes
import prompting.PromptingMain
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class PromptResult(
    val response: String?,
    val error: String?,
)

class JsonRoutesTest : BaseAuthenticationServer() {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Test successful json route with valid request`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "This is a test response",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {}
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns jsonResponse

            // Mock MessageHandler
            mockkObject(MessageHandler)
            coEvery { 
                MessageHandler.saveMessage(any(), any(), any()) 
            } returns ChatMessage(
                conversationId = "test-conversation-id", 
                senderId = "testUser", 
                content = "Test prompt"
            )
            coEvery { MessageHandler.savePrototype(any(), any(), any()) } returns "message-id"

            // Mock JsonProcessor
            mockkObject(JsonProcessor)
            coEvery { JsonProcessor.processRawJsonResponse(any()) } returns Pair("This is a test response", "{}")

            // Mock getPreviousPrototype
            mockkStatic("chat.storage.StorageKt")
            coEvery { 
                getPreviousPrototype(any()) 
            } returns null

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt",
                                "predefined": false
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("This is a test response"))
                assertTrue(responseBody.contains("LLM"))
                assertTrue(responseBody.contains("messageId"))
                assertTrue(responseBody.contains("conversationId"))
            } finally {
                resetPromptingMain()
                unmockkAll()
            }
        }

    @Test
    fun `Test successful json route with predefined response`() =
        testApplication {
            // Mock PredefinedPrototypes
            mockkObject(PredefinedPrototypes)
            coEvery { 
                PredefinedPrototypes.run(any()) 
            } returns PredefinedPrototypeTemplate(
                chatMessage = "This is a predefined response",
                files = """{"file":"content"}"""
            )

            // Mock MessageHandler
            mockkObject(MessageHandler)
            coEvery { 
                MessageHandler.savePrototype(any(), any(), any()) 
            } returns "message-id"

            try {
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt",
                                "predefined": true
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("This is a predefined response"))
                assertTrue(responseBody.contains("LLM"))
                assertTrue(responseBody.contains("messageId"))
                assertTrue(responseBody.contains("conversationId"))
                assertTrue(responseBody.contains("""{"file":"content"}"""))
            } finally {
                unmockkAll()
            }
        }

    @Test
    fun `Test json route with JsonProcessor exception`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "This is a test response",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {}
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns jsonResponse

            // Mock MessageHandler
            mockkObject(MessageHandler)
            coEvery { 
                MessageHandler.saveMessage(any(), any(), any()) 
            } returns ChatMessage(
                conversationId = "test-conversation-id", 
                senderId = "testUser", 
                content = "Test prompt"
            )

            // Mock JsonProcessor to throw exception
            mockkObject(JsonProcessor)
            coEvery { 
                JsonProcessor.processRawJsonResponse(any()) 
            } throws RuntimeException("Error processing JSON")

            // Mock getPreviousPrototype
            mockkStatic("chat.storage.StorageKt")
            coEvery { 
                getPreviousPrototype(any()) 
            } returns null

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt",
                                "predefined": false
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.InternalServerError, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Error processing request"))
            } finally {
                resetPromptingMain()
                unmockkAll()
            }
        }

    @Test
    fun `Test json route with MessageHandler exception`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "This is a test response",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {}
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns jsonResponse

            // Mock MessageHandler
            mockkObject(MessageHandler)
            coEvery { 
                MessageHandler.saveMessage(any(), any(), any()) 
            } returns ChatMessage(
                conversationId = "test-conversation-id", 
                senderId = "testUser", 
                content = "Test prompt"
            )
            coEvery { 
                MessageHandler.savePrototype(any(), any(), any()) 
            } throws RuntimeException("Error saving prototype")

            // Mock JsonProcessor
            mockkObject(JsonProcessor)
            coEvery { 
                JsonProcessor.processRawJsonResponse(any()) 
            } returns Pair("This is a test response", "{}")

            // Mock getPreviousPrototype
            mockkStatic("chat.storage.StorageKt")
            coEvery { 
                getPreviousPrototype(any()) 
            } returns null

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt",
                                "predefined": false
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.InternalServerError, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Error processing request"))
            } finally {
                resetPromptingMain()
                unmockkAll()
            }
        }

    @Test
    fun `Test json route with PredefinedPrototypes exception`() =
        testApplication {
            // Mock PredefinedPrototypes to throw exception
            mockkObject(PredefinedPrototypes)
            coEvery { 
                PredefinedPrototypes.run(any()) 
            } throws RuntimeException("Error with predefined prototype")

            try {
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt",
                                "predefined": true
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.InternalServerError, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Error processing request"))
            } finally {
                unmockkAll()
            }
        }

    @Test
    fun `Test json route with blank prompt`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00",
                            "prompt": "",
                            "predefined": false
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request: Empty prompt"))
        }

    @Test
    fun `Test json route with invalid JSON`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody("This is not valid JSON")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request"))
        }

    @Test
    fun `Test json route with error response from PromptingMain`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "Error processing prompt",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {
                            "file": "content"
                        }
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns jsonResponse

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt"
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Invalid request"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test successful request parsing`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "Valid response",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {
                            "file":"content"
                        }
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns jsonResponse

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt"
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Invalid request"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test request parsing failure`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody("""{"incorrectField": "value"}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request"))
        }

    @Test
    fun `Test resetPromptingMain functionality`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "Mock response",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {
                            "file": "content"
                        }
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns jsonResponse

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val firstResponse =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt"
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.BadRequest, firstResponse.status)
                val firstResponseBody = firstResponse.bodyAsText()
                assertTrue(firstResponseBody.contains("Invalid request"))

                resetPromptingMain()

                val differentMock = mockk<PromptingMain>()
                val differentJsonResponse =
                    """
                    {
                        "chat": {
                            "message": "Default response after reset",
                            "role": "LLM",
                            "timestamp": "2025-01-01T12:00:00",
                            "messageId": "0"
                        },
                        "prototype": {
                            "files": {"file":"content"}
                        }
                    }
                    """.trimIndent()
                coEvery { differentMock.run(any<String>(), anyOrNull<String>()) } returns differentJsonResponse

                setPromptingMain(differentMock)
                val secondResponse =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt"
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.BadRequest, secondResponse.status)
                val secondResponseBody = secondResponse.bodyAsText()
                assertTrue(secondResponseBody.contains("Invalid request"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test missing required fields in request JSON`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00"

                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request"))
        }

    @Test
    fun `Test empty prompt in request`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "Response to empty prompt",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {"file":"content"}
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(eq(""), anyOrNull()) } returns jsonResponse

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": ""
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Invalid request"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test exception thrown by PromptingMain`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            coEvery {
                mockPromptingMain.run(
                    any(),
                    eq(null),
                )
            } throws RuntimeException("Simulation of processing error")

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt",
                                "conversationId": "test-conversation-id"
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Invalid request"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test unauthorized access to json route`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00",
                            "prompt": "Test prompt"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `Test invalid token for authorization`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer invalidToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00",
                            "prompt": "Test prompt"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `Test null prompt in request`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00",
                            "prompt": null
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request"))
        }

    @Test
    fun `Test different exception in receive request`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody("") 
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request"))
        }

    @Test
    fun `Test request with prompt key missing`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request"))
        }

    @Test
    fun `Test successful conversation rename`() =
        testApplication {
            setupTestApplication()
            mockkStatic("chat.storage.StorageKt")
            val conversationId = "123"
            val newName = "New Conversation Name"

            coEvery { updateConversationName(conversationId, newName) } returns true

            val response =
                client.post("/api/chat/json/$conversationId/rename") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "name": "$newName"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Conversation renamed successfully"))
            unmockkAll()
        }

    @Test
    fun `Test request with name key missing`() =
        testApplication {
            setupTestApplication()
            val conversationId = "123"

            val response =
                client.post("/api/chat/json/$conversationId/rename") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {}
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Missing name"))
        }

    @Test
    fun `Test failed conversation rename due to update failure`() =
        testApplication {
            setupTestApplication()
            mockkStatic("chat.storage.StorageKt")
            val conversationId = "123"
            val newName = "New Conversation Name"

            coEvery { updateConversationName(conversationId, newName) } returns false

            val response =
                client.post("/api/chat/json/$conversationId/rename") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "name": "$newName"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to update name"))
            unmockkAll()
        }

    @Test
    fun `Test request with malformed JSON`() =
        testApplication {
            setupTestApplication()
            mockkStatic("chat.storage.StorageKt")
            val conversationId = "123"

            val response =
                client.post("/api/chat/json/$conversationId/rename") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "name": "New Name"
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `Test save prototype`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns
                """
                {
                    "chat": {
                        "message": "Valid response",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {
                            "file": "content"
                        }
                    }
                }
                """.trimIndent()

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response =
                    client.post("/api/chat/json") {
                        header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "userID": "testUser",
                                "time": "2025-01-01T12:00:00",
                                "prompt": "Test prompt"
                            }
                            """.trimIndent(),
                        )
                    }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Invalid request"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test getPromptingMain when instance is already initialized`() =
        testApplication {
            val mockPromptingMain = mockk<PromptingMain>()
            val jsonResponse =
                """
                {
                    "chat": {
                        "message": "Valid response",
                        "role": "LLM",
                        "timestamp": "2025-01-01T12:00:00",
                        "messageId": "0"
                    },
                    "prototype": {
                        "files": {
                            "file": "content"
                        }
                    }
                }
                """.trimIndent()
            coEvery { mockPromptingMain.run(any<String>(), anyOrNull<String>()) } returns jsonResponse
            setPromptingMain(mockPromptingMain)
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00",
                            "prompt": "Test prompt"
                        }
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Invalid request"))
        }
}
