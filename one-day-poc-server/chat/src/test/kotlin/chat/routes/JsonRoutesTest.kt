package chat.routes

import chat.BaseAuthenticationServer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import prompting.ChatResponse
import prompting.PromptingMain
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class PromptResult(
    val response: String?,
    val error: String?,
)

class JsonRoutesTest : BaseAuthenticationServer() {
    //    private fun Application.successTestModule() {
//        setTestClient(createSuccessMockClient())
//        setTestEndpoint("http://test-server/api/prototype/generate")
//        chatModule()
//    }
//
//    private fun Application.errorTestModule() {
//        setTestClient(createErrorMockClient())
//        setTestEndpoint("http://test-server/api/prototype/generate")
//        chatModule()
//    }
//
//    private fun createSuccessMockClient(): HttpClient {
//        return HttpClient(MockEngine) {
//            engine {
//                addHandler { request ->
//                    respond(
//                        content = "Test LLM Response",
//                        status = HttpStatusCode.OK,
//                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
//                    )
//                }
//            }
//        }
//    }
//
//    private fun createErrorMockClient(): HttpClient {
//        return HttpClient(MockEngine) {
//            engine {
//                addHandler { request ->
//                    respond(
//                        content = "Service Error",
//                        status = HttpStatusCode.InternalServerError,
//                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
//                    )
//                }
//            }
//        }
//    }

    @Test
    fun `Test successful json route with valid request`() =
        testApplication {
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(
                    ChatResponse(
                        "This is a test response",
                        "2025-01-01T12:00:00",
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
                assertEquals("This is a test response", responseBody)
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
                    ChatResponse(
                        "Error processing prompt",
                        "2025-01-01T12:00:00",
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
                assertTrue(response.bodyAsText().contains("Error processing prompt"))
            } finally {
                resetPromptingMain()
            }
        }

    @Test
    fun `Test successful request parsing`() =
        testApplication {
            val mockPromptingMain = mock<PromptingMain>()
            runBlocking {
                whenever(mockPromptingMain.run(any())).thenReturn(ChatResponse("Valid response", "2025-01-01T12:00:00"))
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
                assertEquals("Valid response", responseBody)
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
                    ChatResponse(
                        "Mock response",
                        "2025-01-01T12:00:00",
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

                assertEquals("Mock response", firstResponse.bodyAsText())

                resetPromptingMain()

                val differentMock = mock<PromptingMain>()
                runBlocking {
                    whenever(differentMock.run(any())).thenReturn(
                        ChatResponse(
                            "Default response after reset",
                            "2025-01-01T12:00:00",
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

                assertEquals("Default response after reset", secondResponse.bodyAsText())
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
                    ChatResponse(
                        "Response to empty prompt",
                        "2025-01-01T12:00:00",
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

                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals("Response to empty prompt", response.bodyAsText())
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

                val exception = assertThrows<RuntimeException> {
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

                assertEquals("Simulation of processing error", exception.message)
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

                val exception = assertThrows<NullPointerException> {
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
}
