package kcl.seg.rtt.chat

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kcl.seg.rtt.chat.routes.setPromptingMain
import kcl.seg.rtt.chat.routes.resetPromptingMain
import kcl.seg.rtt.prompting.ChatResponse
import kcl.seg.rtt.prompting.PromptingMain

data class PromptResult(val response: String?, val error: String?)

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
    fun `Test successful json route with valid request`() = testApplication {
        val mockPromptingMain = mock<PromptingMain> {
            on { run(any()) } doReturn ChatResponse("This is a test response", "2025-01-01T12:00:00")
        }

        try {
            setPromptingMain(mockPromptingMain)
            setupTestApplication()

            val response = client.post("/api/chat/json") {
                header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "userID": "testUser",
                        "time": "2025-01-01T12:00:00",
                        "prompt": "Test prompt"
                    }
                """.trimIndent())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertEquals("This is a test response", responseBody)

        } finally {
            resetPromptingMain()
        }
    }

    @Test
    fun `Test json route with invalid JSON`() = testApplication {
        setupTestApplication()

        val response = client.post("/api/chat/json") {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("This is not valid JSON")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Invalid request"))
    }

    @Test
    fun `Test json route with error response from PromptingMain`() = testApplication {
        val mockPromptingMain = mock<PromptingMain> {
            on { run(any()) } doReturn ChatResponse("Error processing prompt", "2025-01-01T12:00:00")
        }

        try {
            setPromptingMain(mockPromptingMain)
            setupTestApplication()

            val response = client.post("/api/chat/json") {
                header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "userID": "testUser",
                        "time": "2025-01-01T12:00:00",
                        "prompt": "Test prompt"
                    }
                """.trimIndent())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Error processing prompt"))

        } finally {
            resetPromptingMain()
        }
    }

    @Test
    fun `Test successful request parsing`() = testApplication {
        val mockPromptingMain = mock<PromptingMain> {
            on { run(any()) } doReturn ChatResponse("Valid response", "2025-01-01T12:00:00")
        }

        try {
            setPromptingMain(mockPromptingMain)
            setupTestApplication()

            val response = client.post("/api/chat/json") {
                header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                contentType(ContentType.Application.Json)
                setBody("""
                {
                    "userID": "testUser",
                    "time": "2025-01-01T12:00:00",
                    "prompt": "Test prompt"
                }
            """.trimIndent())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertEquals("Valid response", responseBody)

        } finally {
            resetPromptingMain()
        }
    }

    @Test
    fun `Test request parsing failure`() = testApplication {
        setupTestApplication()

        val response = client.post("/api/chat/json") {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("""{"incorrectField": "value"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Invalid request"))
    }
}