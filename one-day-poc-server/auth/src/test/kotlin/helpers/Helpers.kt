package helpers

import authentication.authentication.AuthenticatedSession
import authentication.authentication.setUpCallbackRoute
import authentication.redis.Redis
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import redis.clients.jedis.Jedis
import utils.aws.AWSUserCredentials
import utils.json.PoCJSON.readJsonFile
import java.util.*

class MockAuthenticationProvider<T : Any>(
    config: Config<T>,
) : AuthenticationProvider(config) {
    private val myConfig: Config<T> = config

    class Config<T : Any>(
        name: String,
    ) : AuthenticationProvider.Config(name) {
        var principal: T? = null
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        myConfig.principal?.let { context.principal(it) }
    }
}

fun <T : Any> AuthenticationConfig.mock(
    name: String,
    configure: MockAuthenticationProvider.Config<T>.() -> Unit,
) {
    val provider = MockAuthenticationProvider(MockAuthenticationProvider.Config<T>(name).apply(configure))
    register(provider)
}

object AuthenticationTestHelpers {
    val jsonConfig: JsonObject =
        readJsonFile("cognito-test.json")

    val urlProvider: JsonObject = jsonConfig["providerLookup"]!!.jsonObject

    private fun setupMockJedis(): Jedis {
        val mockJedis = mockk<Jedis>(relaxed = true)
        val storage = mutableMapOf<String, String>()

        every { mockJedis.setex(any<String>(), any<Long>(), any<String>()) } answers {
            val key = arg<String>(0)
            val value = arg<String>(2)
            storage[key] = value
            "OK"
        }

        every { mockJedis.del(any<String>()) } answers {
            val key = arg<String>(0)
            if (storage.containsKey(key)) {
                storage.remove(key)
                1L
            } else {
                0L
            }
        }

        every { mockJedis.get(any<String>()) } answers {
            val key = arg<String>(0)
            storage[key]
        }

        every { mockJedis.close() } returns Unit

        return mockJedis
    }

    /**
     * Set up a mock Redis for testing by directly mocking the getRedisConnection function.
     * This should be called before any test that interacts with [Redis].
     * @return The mock [Jedis] instance that was set up.
     */
    fun setUpMockRedis(): Jedis {
        val connectionMock = setupMockJedis()
        mockkObject(Redis)
        every { Redis.getRedisConnection() } returns connectionMock
        return connectionMock
    }

    /**
     * Reset the Redis mock.
     * This should be called after any test that uses setupMockRedis.
     */
    fun resetMockRedis(jedis: Jedis) {
        unmockkObject(Redis)
        unmockkObject(jedis)
    }

    /**
     * Set up a mock AWS credentials provider for testing.
     * This should be called before any test that interacts with AWS services.
     * @return The mock AWS credentials provider that was set up.
     */
    fun setupMockAWSCredentials(): MockAWSCredentialsProvider {
        val mockAWSCredentialsProvider = MockAWSCredentialsProvider()
        AWSUserCredentials.setProvider(mockAWSCredentialsProvider)
        return mockAWSCredentialsProvider
    }

    /**
     * Reset the AWS credentials provider to the default.
     * This should be called after any test that uses setupMockAWSCredentials.
     */
    fun resetMockAWSCredentials() {
        AWSUserCredentials.resetProvider()
    }

    fun TestApplicationBuilder.setupExternalServices() {
        externalServices {
            hosts("http://example.com:2000") {
                routing {
                    install(ContentNegotiation) {
                        json()
                    }
                    get("/authorize") {
                        val mockPrincipalJson =
                            """
                            accessToken = "mockAccessToken",
                            tokenType = "Bearer",
                            expiresIn = 3600,
                            refreshToken = "mockRefreshToken",
                            """.trimIndent()
                        call.respond(mockPrincipalJson)
                    }
                }
            }
        }
    }

    /**
     * Set up Basic authentication for testing purposes only
     */
    fun AuthenticationConfig.configureBasicAuthentication() {
        basic("testAuth") {
            validate { credentials ->
                if (credentials.name == "test" && credentials.password == "password") {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

    fun Application.configureTestCallbackRoute() {
        install(ContentNegotiation) {
            json()
        }
        install(Sessions) {
            cookie<AuthenticatedSession>("AuthenticatedSession")
        }
        install(Authentication) {
            mock<OAuthAccessTokenResponse.OAuth2>("test") {
                principal =
                    OAuthAccessTokenResponse.OAuth2(
                        accessToken = "dummy_access",
                        tokenType = "Bearer",
                        expiresIn = Long.MAX_VALUE,
                        refreshToken = null,
                        extraParameters =
                            Parameters.build {
                                append("id_token", generateTestJwtToken())
                            },
                    )
            }
        }
        routing {
            authenticate("test") {
                setUpCallbackRoute("/validate", "localhost:7000")
            }
        }
    }

    fun generateTestJwtToken(): String =
        JWT
            .create()
            .withIssuer("test-issuer")
            .withSubject("user123")
            .withClaim("cognito:groups", listOf("admin_users"))
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))

    fun generateTestJwtTokenAdminFalse(): String =
        JWT
            .create()
            .withIssuer("test-issuer")
            .withSubject("user123")
            .withClaim("cognito:groups", listOf("regular_users"))
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))

    fun generateTestJwtTokenNoSub(): String =
        JWT
            .create()
            .withIssuer("test-issuer")
            .withClaim("cognito:groups", listOf("admin_users"))
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))

    fun generateTestJwtTokenNoGroups(): String =
        JWT
            .create()
            .withIssuer("test-issuer")
            .withSubject("user123")
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))
}
