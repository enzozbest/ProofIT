import authentication.authentication.AuthenticatedSession
import authentication.authentication.setUpCallbackRoute
import helpers.AuthenticationTestHelpers.configureTestCallbackRoute
import helpers.AuthenticationTestHelpers.generateTestJwtTokenAdminFalse
import helpers.AuthenticationTestHelpers.generateTestJwtTokenNoGroups
import helpers.AuthenticationTestHelpers.generateTestJwtTokenNoSub
import helpers.AuthenticationTestHelpers.resetMockRedis
import helpers.AuthenticationTestHelpers.setUpMockRedis
import helpers.mock
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class TestCallbackRoute {
    @BeforeEach
    fun setUp() {
        setUpMockRedis()
    }

    @AfterEach
    fun tearDown() {
        resetMockRedis()
    }

    @Test
    fun `Test Callback route redirects when principal is present`() =
        testApplication {
            application {
                configureTestCallbackRoute()
            }
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get("/validate")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("localhost:7000/", response.headers["Location"])

            val setCookieHeaders: List<String>? = response.headers.getAll(HttpHeaders.SetCookie)
            assertNotNull(setCookieHeaders, "Expected at least one Set-Cookie header.")

            val sessionCookieHeader = setCookieHeaders.find { it.startsWith("AuthenticatedSession") }
            assertNotNull(sessionCookieHeader, "AuthenticatedSessioncookie not found.")
        }

    @Test
    fun `Test Callback route returns Unauthorized when principal is missing`() =
        testApplication {
            install(Authentication) {
                mock<OAuthAccessTokenResponse.OAuth2>("test") {
                    principal = null
                }
            }
            routing {
                authenticate("test") {
                    setUpCallbackRoute("/validate")
                }
            }
            val response = client.get("/validate")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test Callback route returns Unauthorized when principal has no extra parameters`() =
        testApplication {
            install(Authentication) {
                mock<OAuthAccessTokenResponse.OAuth2>("test") {
                    principal =
                        OAuthAccessTokenResponse.OAuth2(
                            accessToken = "dummy_access",
                            tokenType = "Bearer",
                            expiresIn = Long.MAX_VALUE,
                            refreshToken = null,
                        )
                }
            }
            routing {
                authenticate("test") {
                    setUpCallbackRoute("/validate")
                }
            }
            val response = client.get("/validate")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test Callback route returns Unauthorized when token is empty`() =
        testApplication {
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
                                    append("id_token", "")
                                },
                        )
                }
            }
            routing {
                authenticate("test") {
                    setUpCallbackRoute("/validate")
                }
            }
            val response = client.get("/validate")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test Callback route returns Unauthorized when JWT has no sub`() =
        testApplication {
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
                                    append("id_token", generateTestJwtTokenNoSub())
                                },
                        )
                }
            }
            routing {
                authenticate("test") {
                    setUpCallbackRoute("/validate")
                }
            }
            val response = client.get("/validate")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test Callback route with redirect parameter`() =
        testApplication {
            application {
                configureTestCallbackRoute()
            }
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get("/validate?redirect=/test")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("localhost:7000/test", response.headers["Location"])
        }

    @Test
    fun `Test Callback route with no admin flag`() =
        testApplication {
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
                                    append("id_token", generateTestJwtTokenNoGroups())
                                },
                        )
                }
            }
            routing {
                authenticate("test") {
                    setUpCallbackRoute("/validate")
                }
            }
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get("/validate")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("http://localhost:5173/", response.headers["Location"])

            val setCookieHeaders: List<String>? = response.headers.getAll(HttpHeaders.SetCookie)
            assertNotNull(setCookieHeaders, "Expected at least one Set-Cookie header.")

            val sessionCookieHeader = setCookieHeaders.find { it.startsWith("AuthenticatedSession=") }
            assertNotNull(sessionCookieHeader, "AuthenticatedSession cookie not found.")

            val cookieValueEncoded =
                URLDecoder
                    .decode(sessionCookieHeader, "UTF-8")
                    .substringAfter("AuthenticatedSession=")
                    .substringBefore(";")

            val decoded = Json.decodeFromString<AuthenticatedSession>(cookieValueEncoded)
            assertFalse(decoded.admin == true)
        }

    @Test
    fun `Test Callback route with admin false flag`() =
        testApplication {
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
                                    append("id_token", generateTestJwtTokenAdminFalse())
                                },
                        )
                }
            }
            routing {
                authenticate("test") {
                    setUpCallbackRoute("/validate")
                }
            }
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get("/validate")
            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals("http://localhost:5173/", response.headers["Location"])

            val setCookieHeaders: List<String>? = response.headers.getAll(HttpHeaders.SetCookie)
            assertNotNull(setCookieHeaders, "Expected at least one Set-Cookie header.")

            val sessionCookieHeader = setCookieHeaders.find { it.startsWith("AuthenticatedSession=") }
            assertNotNull(sessionCookieHeader, "AuthenticatedSession cookie not found.")

            val cookieValueEncoded =
                URLDecoder
                    .decode(sessionCookieHeader, "UTF-8")
                    .substringAfter("AuthenticatedSession=")
                    .substringBefore(";")

            val decoded = Json.decodeFromString<AuthenticatedSession>(cookieValueEncoded)
            assertFalse(decoded.admin == true)
        }
}
