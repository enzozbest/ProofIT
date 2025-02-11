import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kcl.seg.rtt.auth.AuthenticatedSession
import kcl.seg.rtt.auth.LOG_OUT_ROUTE
import kcl.seg.rtt.auth.authModule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestLogoutRoute {
    @Test
    fun `Test Log Out Route clears session`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val response = client.post(LOG_OUT_ROUTE) {
            cookie(
                "AuthenticatedSession",
                Json.encodeToString(AuthenticatedSession("test", "test", false))
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val setCookieHeader = response.headers[HttpHeaders.SetCookie]
        assertNotNull(setCookieHeader, "Set-Cookie header should be present")
        assertTrue(setCookieHeader.contains("AuthenticatedSession=;"), "Session cookie should be cleared")
        assertTrue(
            setCookieHeader.contains("Expires=Thu, 01 Jan 1970"),
            "Cookie should have an expiration date in the past"
        )
    }
}
