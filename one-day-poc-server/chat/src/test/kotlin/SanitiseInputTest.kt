package kcl.seg.rtt.chat

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SanitiseInputTest : BaseAuthenticationServer() {
    @Test
    fun `Test json response with clean input`() = testApplication {
        setupTestApplication()

        val requestData = """
            {
                "prompt": "Simple prompt",
                "userID": "user123",
                "time": "2025-02-02T12:00:00"
            }
        """

        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestData)
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Simple prompt"))
    }

    @Test
    fun `Test json response with HTML in input`() = testApplication {
        setupTestApplication()

        val requestData = """
            {
                "prompt": "<div>Simple <b>test</b> prompt &amp; with HTML.</div>",
                "userID": "user123",
                "time": "2025-02-02T12:00:00"
            }
        """


        val response = client.post(JSON) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestData)
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Simple test prompt  with HTML."))
    }
}