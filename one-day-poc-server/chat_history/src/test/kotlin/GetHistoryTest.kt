package kcl.seg.rtt.chat_history

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class GetHistoryTest : BaseAuthenticationServer() {
    @Test
    fun `Test unauthorized access`() = testApplication {
        setupTestApplication()
        val response = client.get(GET)
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Test authorized access`() = testApplication {
        setupTestApplication()
        val response = client.get(GET) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `Test invalid token`() = testApplication {
        setupTestApplication()
        val response = client.get(GET) {
            header(HttpHeaders.Authorization, "Bearer invalid-token")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}