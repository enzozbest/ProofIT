package kcl.seg.rtt.chat_history

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class SanitiseInputTest {
    @Test
    fun `Test json response with clean input`() = testApplication {
        application {
            chatModule()
        }

        val requestData = """
            {
                "prompt": "Simple prompt",
                "userID": "user123",
                "time": "2025-02-02T12:00:00"
            }
        """

        val response = client.post("/json") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestData)
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Simple prompt"))
    }

    @Test
    fun `Test json response with HTML in input`() = testApplication {
        application {
            chatModule()
        }

        val requestData = """
            {
                "prompt": "<div>Simple <b>test</b> prompt &amp; with HTML.</div>",
                "userID": "user123",
                "time": "2025-02-02T12:00:00"
            }
        """


        val response = client.post("/json") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestData)
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Simple test prompt  with HTML."))
    }
}