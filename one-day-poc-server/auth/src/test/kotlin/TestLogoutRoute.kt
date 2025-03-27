import authentication.authentication.AuthenticatedSession
import authentication.authentication.setLogOutEndpoint
import authentication.redis.Redis
import helpers.AuthenticationTestHelpers.resetMockRedis
import helpers.AuthenticationTestHelpers.setUpMockRedis
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
    fun `Test logout route with valid session cookie`() =
        testApplication {
            application {
                install(Sessions) {
                    cookie<AuthenticatedSession>("AuthenticatedSession")
                }
            }
            routing {
                setLogOutEndpoint("/logout")
            }

            val token = "test-token"
            val session = AuthenticatedSession("user123", token, false)
            val sessionJson = Json.encodeToString(session)

            Redis.getRedisConnection().use { jedis ->
                jedis.setex("auth:$token", 3600, "cached-data")
            }

            val response =
                client.post("/logout") {
                    header(HttpHeaders.Cookie, "AuthenticatedSession=$sessionJson")
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val setCookieHeaders = response.headers.getAll(HttpHeaders.SetCookie)
            assertNotNull(setCookieHeaders, "Expected at least one Set-Cookie header")

            val authCookie = setCookieHeaders.find { it.startsWith("AuthenticatedSession=") }
            assertNotNull(authCookie, "AuthenticatedSession cookie not found")

            Redis.getRedisConnection().use { jedis ->
                val cachedData = jedis["auth:$token"]
                assertEquals(null, cachedData, "Session should be removed from Redis")
            }
        }

    @Test
    fun `Test logout route with invalid session cookie`() =
        testApplication {
            application {
                install(Sessions) {
                    cookie<AuthenticatedSession>("AuthenticatedSession")
                }
            }
            routing {
                setLogOutEndpoint("/logout")
            }

            val response =
                client.post("/logout") {
                    header(HttpHeaders.Cookie, "AuthenticatedSession=invalid-json")
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test logout route without session cookie`() =
        testApplication {
            application {
                install(Sessions) {
                    cookie<AuthenticatedSession>("AuthenticatedSession")
                }
            }
            routing {
                setLogOutEndpoint("/logout")
            }

            val response = client.post("/logout")

            assertEquals(HttpStatusCode.OK, response.status)
        }
}
