package kcl.seg.rtt.chat_history

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonRoutesTest : BaseAuthenticationServer() {
    @Test
    fun `Test Valid JSON message`() = testApplication {
        setupTestApplication()

        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "userID": "testUser",
                    "time": "2025-01-01T12:00:00",
                    "prompt": "Hello"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Hello, testUser!"))
    }

    @Test
    fun `Test Invalid JSON message`() = testApplication {
        setupTestApplication()

        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "invalid": "json"
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}