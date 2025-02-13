package helpers

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
<<<<<<< HEAD
import kcl.seg.rtt.auth.AuthenticatedSession
import kcl.seg.rtt.auth.setUpCallbackRoute
import kcl.seg.rtt.utils.JSON.PoCJSON.readJsonFile
=======
import kcl.seg.rtt.auth.authentication.AuthenticatedSession
import kcl.seg.rtt.auth.authentication.setUpCallbackRoute
import kcl.seg.rtt.utils.json.PoCJSON.readJsonFile
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.*

<<<<<<< HEAD
class MockAuthenticationProvider<T : Any>(config: Config<T>) :
    AuthenticationProvider(config) {
    private val myConfig: Config<T> = config

    class Config<T : Any>(name: String) : AuthenticationProvider.Config(name) {
=======
class MockAuthenticationProvider<T : Any>(
    config: Config<T>,
) : AuthenticationProvider(config) {
    private val myConfig: Config<T> = config

    class Config<T : Any>(
        name: String,
    ) : AuthenticationProvider.Config(name) {
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        var principal: T? = null
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        myConfig.principal?.let { context.principal(it) }
    }
}

<<<<<<< HEAD
fun <T : Any> AuthenticationConfig.mock(name: String, configure: MockAuthenticationProvider.Config<T>.() -> Unit) {
=======
fun <T : Any> AuthenticationConfig.mock(
    name: String,
    configure: MockAuthenticationProvider.Config<T>.() -> Unit,
) {
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
    val provider = MockAuthenticationProvider(MockAuthenticationProvider.Config<T>(name).apply(configure))
    register(provider)
}

object AuthenticationTestHelpers {
<<<<<<< HEAD

=======
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
    val jsonConfig: JsonObject =
        readJsonFile("src/test/resources/cognito-test.json")

    val urlProvider: JsonObject = jsonConfig["providerLookup"]!!.jsonObject

    fun TestApplicationBuilder.setupExternalServices() {
        externalServices {
            hosts("http://example.com:2000") {
                routing {
                    install(ContentNegotiation) {
                        json()
                    }
                    get("/authorize") {
<<<<<<< HEAD
                        val mockPrincipalJson = """
=======
                        val mockPrincipalJson =
                            """
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
                            accessToken = "mockAccessToken",
                            tokenType = "Bearer",
                            expiresIn = 3600,
                            refreshToken = "mockRefreshToken",
<<<<<<< HEAD
                        """.trimIndent()
=======
                            """.trimIndent()
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
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
<<<<<<< HEAD
                } else null
=======
                } else {
                    null
                }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
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
<<<<<<< HEAD
                principal = OAuthAccessTokenResponse.OAuth2(
                    accessToken = "dummy_access",
                    tokenType = "Bearer",
                    expiresIn = Long.MAX_VALUE,
                    refreshToken = null,
                    extraParameters = Parameters.build {
                        append("id_token", generateTestJwtToken())
                    }
                )
=======
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
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
            }
        }
        routing {
            authenticate("test") {
                setUpCallbackRoute("/validate", "localhost:7000")
            }
        }
    }

<<<<<<< HEAD
    private fun generateTestJwtToken(): String {
        return JWT.create()
=======
    fun generateTestJwtToken(): String =
        JWT
            .create()
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
            .withIssuer("test-issuer")
            .withSubject("user123")
            .withClaim("cognito:groups", listOf("admin_users"))
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))
<<<<<<< HEAD
    }

    fun generateTestJwtTokenAdminFalse(): String {
        return JWT.create()
=======

    fun generateTestJwtTokenAdminFalse(): String =
        JWT
            .create()
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
            .withIssuer("test-issuer")
            .withSubject("user123")
            .withClaim("cognito:groups", listOf("regular_users"))
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))
<<<<<<< HEAD
    }

    fun generateTestJwtTokenNoSub(): String {
        return JWT.create()
=======

    fun generateTestJwtTokenNoSub(): String =
        JWT
            .create()
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
            .withIssuer("test-issuer")
            .withClaim("cognito:groups", listOf("admin_users"))
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))
<<<<<<< HEAD
    }

    fun generateTestJwtTokenNoGroups(): String {
        return JWT.create()
=======

    fun generateTestJwtTokenNoGroups(): String =
        JWT
            .create()
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
            .withIssuer("test-issuer")
            .withSubject("user123")
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256("test-secret"))
<<<<<<< HEAD
    }
}


=======
}
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
