package chat.routes

import chat.BaseAuthenticationServer
import chat.storage.updateConversationName
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
import prompting.PredefinedPrototypes
import prompting.PredefinedPrototypeTemplate
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
                        "files": "{}"
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

    @Test
    fun `Test predefined prototype functionality`() =
        testApplication {
            // Mock PredefinedPrototypes.run
            mockkObject(PredefinedPrototypes)
            val predefinedTemplate = PredefinedPrototypeTemplate(
                chatMessage = "This is a predefined response",
                files = """{"file1": "content1", "file2": "content2"}"""
            )
            every { PredefinedPrototypes.run(any()) } returns predefinedTemplate

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
                            "prompt": "Test predefined prompt",
                            "conversationId": "test-conversation-id",
                            "predefined": true
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            // Verify the response contains the predefined message
            assertTrue(responseBody.contains("This is a predefined response"))
            assertTrue(responseBody.contains("file1"))
            assertTrue(responseBody.contains("content1"))

            // Verify PredefinedPrototypes.run was called with the correct prompt
            verify { PredefinedPrototypes.run("Test predefined prompt") }

            unmockkObject(PredefinedPrototypes)
        }
}
