package kcl.seg.rtt.chat

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kcl.seg.rtt.chat.routes.sanitisePrompt
import kcl.seg.rtt.chat.routes.setTestClient
import kcl.seg.rtt.chat.routes.setTestEndpoint
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val PROTOTYPE_ENDPOINT = "/api/prototype/generate"

class JsonRoutesTest : BaseAuthenticationServer() {

    private fun Application.successTestModule() {
        setTestClient(createSuccessMockClient())
        setTestEndpoint("http://test-server/api/prototype/generate")
        chatModule()
    }

    private fun Application.errorTestModule() {
        setTestClient(createErrorMockClient())
        setTestEndpoint("http://test-server/api/prototype/generate")
        chatModule()
    }

    private fun createSuccessMockClient(): HttpClient {
        print("SUCCESS MOCK CLIENT")
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = "Test LLM Response",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
        }
    }

    private fun createErrorMockClient(): HttpClient {
        "ERROR MOCK CLIENT"
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = "Service Error",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
        }
    }

    @Test
    fun `Test sanitizePrompt removes HTML and special characters`() {
        val input = "<script>alert('test')</script>&nbsp;test"
        val result = sanitisePrompt(input)
        assertEquals("test", result)
    }

    @Test
    fun `Test successful prototype request`() = testApplication {
        setupTestApplication { successTestModule() }

        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "userID": "testUser",
                    "time": "2025-01-01T12:00:00",
                    "prompt": "Create a login form"
                }
            """.trimIndent())
        }
        val responseText = response.bodyAsText()
        println("Response: $responseText")
        assertEquals(HttpStatusCode.OK, response.status)

        assertTrue(responseText.contains("time"))
        assertTrue(responseText.contains("message"))
    }

    @Test
    fun `Test error handling in prototype request`() = testApplication {
        setupTestApplication { errorTestModule() }

        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "userID": "testUser",
                    "time": "2025-01-01T12:00:00",
                    "prompt": "Create a login form"
                }
            """.trimIndent())
        }
        val responseText = response.bodyAsText()
        println("Response: $responseText")

        // tests both 200 ok with error message and 500 internal server error depending on our final implementation
        assertTrue(responseText.contains("Error") || response.status == HttpStatusCode.InternalServerError)
    }

    @Test
    fun `Test invalid JSON request handling`() = testApplication {
        setupTestApplication { successTestModule() }

        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("invalid json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("Invalid request"))
    }

    @Test
    fun `Test network error handling`() = testApplication {
        setupTestApplication {
            setTestClient(HttpClient(MockEngine) {
                engine {
                    addHandler { _ ->
                        throw java.net.ConnectException("Connection refused")
                    }
                }
            })
            chatModule()
        }

        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("""
            {
                "userID": "testUser",
                "time": "2025-01-01T12:00:00",
                "prompt": "Create a login form"
            }
        """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("Connection refused"))
    }
}