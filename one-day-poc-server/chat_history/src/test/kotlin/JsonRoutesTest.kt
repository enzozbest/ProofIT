package kcl.seg.rtt.chat_history

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class JsonRoutesTest {
    @Test
    fun testValidJsonRequest() = testApplication {
        application {
            chatModule()
        }

        val response = client.post("/json") {
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
    fun testInvalidJsonRequest() = testApplication {
        application {
            chatModule()
        }

        val response = client.post("/json") {
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