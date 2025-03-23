package chat.routes

import chat.BaseAuthenticationServer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import prompting.ChatResponse
import prompting.PromptingMain
import prompting.ServerResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class PromptResult(
    val response: String?,
    val error: String?,
)

class JsonRoutesTest : BaseAuthenticationServer() {
    @Test
    fun `Test successful json route with valid request`() =
        testApplication {
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(
                    ServerResponse(
                        chat =
                            ChatResponse(
                                message = "This is a test response",
                                timestamp = "2025-01-01T12:00:00",
                                messageId = "0",
                            ),
                    ),
                )
            }

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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseBody = response.bodyAsText()
                val serverResponse = Json.decodeFromString<ServerResponse>(responseBody)
                assertEquals("This is a test response", serverResponse.chat.message)
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
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(
                    ServerResponse(
                        chat =
                            ChatResponse(
                                message = "Error processing prompt",
                                timestamp = "2025-01-01T12:00:00",
                                messageId = "0",
                            ),
                    ),
                )
            }

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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseBody = response.bodyAsText()
                val serverResponse = Json.decodeFromString<ServerResponse>(responseBody)
                assertTrue(serverResponse.chat.message.contains("Error processing prompt"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test successful request parsing`() =
        testApplication {
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(
                    ServerResponse(
                        chat =
                            ChatResponse(
                                message = "Valid response",
                                timestamp = "2025-01-01T12:00:00",
                                messageId = "0",
                            ),
                    ),
                )
            }

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

                assertEquals(HttpStatusCode.OK, response.status)
                val responseBody = response.bodyAsText()
                val serverResponse = Json.decodeFromString<ServerResponse>(responseBody)
                assertEquals("Valid response", serverResponse.chat.message)
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
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(
                    ServerResponse(
                        chat =
                            ChatResponse(
                                message = "Mock response",
                                timestamp = "2025-01-01T12:00:00",
                                messageId = "0",
                            ),
                    ),
                )
            }

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

                val firstResponseBody = firstResponse.bodyAsText()
                val firstServerResponse = Json.decodeFromString<ServerResponse>(firstResponseBody)
                assertEquals("Mock response", firstServerResponse.chat.message)

                resetPromptingMain()

                val differentMock = mock<PromptingMain>()
                runBlocking {
                    whenever(differentMock.run(any())).thenReturn(
                        ServerResponse(
                            chat =
                                ChatResponse(
                                    message = "Default response after reset",
                                    timestamp = "2025-01-01T12:00:00",
                                    messageId = "0",
                                ),
                        ),
                    )
                }

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

                val secondResponseBody = secondResponse.bodyAsText()
                val secondServerResponse = Json.decodeFromString<ServerResponse>(secondResponseBody)
                assertEquals("Default response after reset", secondServerResponse.chat.message)
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
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run("")).thenReturn(
                    ServerResponse(
                        chat =
                            ChatResponse(
                                message = "Response to empty prompt",
                                timestamp = "2025-01-01T12:00:00",
                                messageId = "0",
                            ),
                    ),
                )
            }

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
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenThrow(RuntimeException("Simulation of processing error"))
            }

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                val response = client.post("/api/chat/json") {
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

                assertEquals(HttpStatusCode.InternalServerError, response.status)
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.contains("Error") || responseBody.contains("error"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test null return value from PromptingMain run method`() =
        testApplication {
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(null)
            }

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                assertThrows<NullPointerException> {
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
                }
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

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test handling null return from PromptingMain run method`() =
        testApplication {
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(null)
            }

            try {
                setPromptingMain(mockPromptingMain)
                setupTestApplication()

                assertThrows<NullPointerException> {
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
                }
            } finally {
                resetPromptingMain()
            }
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

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test malformed authorization header`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post("/api/chat/json") {
                    header(HttpHeaders.Authorization, "InvalidHeaderFormat")
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

            assertEquals(HttpStatusCode.Unauthorized, response.status)
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
                    setBody("") // Empty body should cause a different exception than malformed JSON
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
}
