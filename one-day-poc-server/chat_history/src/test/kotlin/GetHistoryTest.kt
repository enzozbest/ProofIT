package kcl.seg.rtt.chat_history

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class ChatRoutesTest {
    @Test
    fun testChatEndpoint() = testApplication {
        application {
            chatModule()
        }

        val response = client.get("/chat")
        assertEquals("Hello, world!", response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
    }
}