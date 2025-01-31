import AuthenticationTestHelpers.setupExternalServices
import AuthenticationTestHelpers.urlProvider
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kcl.seg.rtt.auth.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthenticationRoutesTest {

    @Test
    fun `Test Authentication Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(AUTHENTICATION_ROUTE)
        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider["authorizeUrl"]!!.jsonPrimitive.content))
    }

    @Test
    fun `Test Callback Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(CALL_BACK_ROUTE)
        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider["authorizeUrl"]!!.jsonPrimitive.content))
    }

    @Test
    fun `Test Authorize Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        setupExternalServices()
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(urlProvider["authorizeUrl"]!!.jsonPrimitive.content)
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `Test JWT Validation Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(JWT_VALIDATION_ROUTE)
        assertNotNull(response)
    }

    @Test
    fun `Test UserInfo Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val response = client.get(USER_INFO_ROUTE)
        assertNotNull(response)
    }

    @Test
    fun `Test Log Out Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val response = client.get(LOG_OUT_ROUTE)
        assertNotNull(response)
    }

    @Test
    fun `Test Log Out Route clears session`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val response = client.get(LOG_OUT_ROUTE) {
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