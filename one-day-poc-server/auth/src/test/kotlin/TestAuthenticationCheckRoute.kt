import authentication.authentication.AuthenticatedSession
import authentication.authentication.JWTValidationResponse
import authentication.authentication.cacheSession
import authentication.authentication.setUpCheckEndpoint
import authentication.redis.Redis
import helpers.AuthenticationTestHelpers
import helpers.AuthenticationTestHelpers.resetMockRedis
import helpers.AuthenticationTestHelpers.setUpMockRedis
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import kotlin.test.assertEquals

class TestAuthenticationCheckRoute {
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
    fun `Test check route without credentials`() =
        testApplication {
            routing {
                setUpCheckEndpoint("/check")
            }

            val response = client.get("/check")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Invalid or missing session cookie", response.bodyAsText())
        }

    @Test
    fun `Test check route with invalid cookie`() =
        testApplication {
            routing {
                setUpCheckEndpoint("/check")
            }

            val response =
                client.get("/check") {
                    cookie("AuthenticatedSession", "invalid-cookie")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Invalid or missing session cookie", response.bodyAsText())
        }

    @Test
    fun `Test check route with valid cookie, no cache`() =
        testApplication {
            routing {
                setUpCheckEndpoint("/check")
            }
            val jwt = AuthenticationTestHelpers.generateTestJwtToken()
            val sessionCookie =
                AuthenticatedSession(
                    userId = "user123",
                    token = jwt,
                    admin = true,
                )
            val client =
                createClient {
                    followRedirects = false
                }
            val response =
                client.get("/check") {
                    cookie("AuthenticatedSession", Json.encodeToString(sessionCookie))
                }
            assertEquals(HttpStatusCode.TemporaryRedirect, response.status)
            assertEquals("Bearer $jwt", response.headers["Authorization"])
            assertEquals("http://localhost:8000/api/auth/validate", response.headers["Location"])
        }

    @Test
    fun `Test check route with valid cookie, cached`() =
        testApplication {
            install(ContentNegotiation) {
                json()
            }
            routing {
                setUpCheckEndpoint("/check")
            }
            val jwt = AuthenticationTestHelpers.generateTestJwtToken()
            val sessionCookie =
                AuthenticatedSession(
                    userId = "user123",
                    token = jwt,
                    admin = true,
                )
            val validationResponse = JWTValidationResponse("user123", true)
            cacheSession(jwt, validationResponse)
            val response =
                client.get("/check") {
                    cookie("AuthenticatedSession", Json.encodeToString(sessionCookie))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(validationResponse, Json.decodeFromString<JWTValidationResponse>(response.bodyAsText()))
            Redis.getRedisConnection().del("auth:$jwt") // Clean up Redis
        }
}
