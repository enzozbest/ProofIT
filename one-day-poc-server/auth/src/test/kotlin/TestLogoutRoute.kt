import authentication.authentication.AuthenticatedSession
import authentication.authentication.AuthenticationRoutes.LOG_OUT_ROUTE
import authentication.authentication.authModule
import helpers.AuthenticationTestHelpers.resetMockRedis
import helpers.AuthenticationTestHelpers.setUpMockRedis
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestLogoutRoute {
    private lateinit var mockJedis: Jedis

    @BeforeEach
    fun setUp() {
        mockJedis = setUpMockRedis()
    }

    @AfterEach
    fun tearDown() {
        resetMockRedis(mockJedis)
    }

    @Test
    fun `Test Log Out Route clears session`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "cognito-test.json",
                    authName = "testAuth",
                )
            }
            val response =
                client.post(LOG_OUT_ROUTE) {
                    cookie(
                        "AuthenticatedSession",
                        Json.encodeToString(AuthenticatedSession("test", "test", false)),
                    )
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val setCookieHeader = response.headers[HttpHeaders.SetCookie]
            assertNotNull(setCookieHeader, "Set-Cookie header should be present")
            assertTrue(setCookieHeader.contains("AuthenticatedSession=;"), "Session cookie should be cleared")
            assertTrue(
                setCookieHeader.contains("Expires=Thu, 01 Jan 1970"),
                "Cookie should have an expiration date in the past",
            )
        }

    @Test
    fun `Test Logout route without session cookie`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "cognito-test.json",
                    authName = "testAuth",
                )
            }
            val response = client.post(LOG_OUT_ROUTE)
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test Logout route with invalid session cookie`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "cognito-test.json",
                    authName = "testAuth",
                )
            }
            val response =
                client.post(LOG_OUT_ROUTE) {
                    cookie("AuthenticatedSession", "invalid")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }
}
