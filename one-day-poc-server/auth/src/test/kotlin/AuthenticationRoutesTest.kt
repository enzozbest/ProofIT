import helpers.AuthenticationTestHelpers.configureTestCallbackRoute
import helpers.AuthenticationTestHelpers.generateTestJwtTokenAdminFalse
import helpers.AuthenticationTestHelpers.generateTestJwtTokenNoGroups
import helpers.AuthenticationTestHelpers.generateTestJwtTokenNoSub
import helpers.AuthenticationTestHelpers.setupExternalServices
import helpers.AuthenticationTestHelpers.urlProvider
import helpers.mock
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kcl.seg.rtt.auth.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import kotlin.test.*

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


    @Test
    fun `Test JWT validation route returns expected session details`() = testApplication {
        application {
            this@application.install(ContentNegotiation) {
                json()
            }
            this@application.routing {
                setUpJWTValidation("/validate")
            }
        }
        val session = AuthenticatedSession(userId = "user123", token = "tokenABC", admin = true)
        val cookieValue = Json.encodeToString<AuthenticatedSession>(session)

        val response = client.get("/validate") {
            cookie("AuthenticatedSession", cookieValue)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()

        val expectedJson = Json.parseToJsonElement("""{"userId":"user123","admin":true}""")
        val actualJson = Json.parseToJsonElement(responseBody)
        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `Test JWT validation route returns empty response without cookie`() = testApplication {
        application {
            this@application.install(ContentNegotiation) {
                json()
            }
            this@application.routing {
                setUpJWTValidation("/validate")
            }
        }
        assertFailsWith<SerializationException> {
            client.get("/validate")
        }
    }

    @Test
    fun `Test Callback route redirects when principal is present`() = testApplication {
        application {
            configureTestCallbackRoute()
        }
        val myClient = createClient {
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
    fun `Test Callback route returns Unauthorized when principal is missing`() = testApplication {
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
    fun `Test Callback route returns unauthorized when principal has no extra parameters`() = testApplication {
        install(Authentication) {
            mock<OAuthAccessTokenResponse.OAuth2>("test") {
                principal = OAuthAccessTokenResponse.OAuth2(
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
    fun `Test Callback route returns unauthorized when token is empty`() = testApplication {
        install(Authentication) {
            mock<OAuthAccessTokenResponse.OAuth2>("test") {
                principal = OAuthAccessTokenResponse.OAuth2(
                    accessToken = "dummy_access",
                    tokenType = "Bearer",
                    expiresIn = Long.MAX_VALUE,
                    refreshToken = null,
                    extraParameters = Parameters.build {
                        append("id_token", "")
                    }
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
    fun `Test Callback route returns unauthorized when JWT has no sub`() = testApplication {
        install(Authentication) {
            mock<OAuthAccessTokenResponse.OAuth2>("test") {
                principal = OAuthAccessTokenResponse.OAuth2(
                    accessToken = "dummy_access",
                    tokenType = "Bearer",
                    expiresIn = Long.MAX_VALUE,
                    refreshToken = null,
                    extraParameters = Parameters.build {
                        append("id_token", generateTestJwtTokenNoSub())
                    }
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
    fun `Test Callback route with redirect parameter`() = testApplication {
        application {
            configureTestCallbackRoute()
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get("/validate?redirect=/test")
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("localhost:7000/test", response.headers["Location"])
    }

    @Test
    fun `Test Callback route with no admin flag`() = testApplication {
        install(Sessions) {
            cookie<AuthenticatedSession>("AuthenticatedSession")
        }
        install(Authentication) {
            mock<OAuthAccessTokenResponse.OAuth2>("test") {
                principal = OAuthAccessTokenResponse.OAuth2(
                    accessToken = "dummy_access",
                    tokenType = "Bearer",
                    expiresIn = Long.MAX_VALUE,
                    refreshToken = null,
                    extraParameters = Parameters.build {
                        append("id_token", generateTestJwtTokenNoGroups())
                    }
                )
            }
        }
        routing {
            authenticate("test") {
                setUpCallbackRoute("/validate")
            }
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get("/validate")
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("http://localhost:5173/", response.headers["Location"])

        val setCookieHeaders: List<String>? = response.headers.getAll(HttpHeaders.SetCookie)
        assertNotNull(setCookieHeaders, "Expected at least one Set-Cookie header.")

        val sessionCookieHeader = setCookieHeaders.find { it.startsWith("AuthenticatedSession=") }
        assertNotNull(sessionCookieHeader, "AuthenticatedSession cookie not found.")

        val cookieValueEncoded = URLDecoder.decode(sessionCookieHeader, "UTF-8")
            .substringAfter("AuthenticatedSession=")
            .substringBefore(";")

        val decoded = Json.decodeFromString<AuthenticatedSession>(cookieValueEncoded)
        assertFalse(decoded.admin ?: false)
    }

    @Test
    fun `Test Callback route with admin false flag`() = testApplication {
        install(Sessions) {
            cookie<AuthenticatedSession>("AuthenticatedSession")
        }
        install(Authentication) {
            mock<OAuthAccessTokenResponse.OAuth2>("test") {
                principal = OAuthAccessTokenResponse.OAuth2(
                    accessToken = "dummy_access",
                    tokenType = "Bearer",
                    expiresIn = Long.MAX_VALUE,
                    refreshToken = null,
                    extraParameters = Parameters.build {
                        append("id_token", generateTestJwtTokenAdminFalse())
                    }
                )
            }
        }
        routing {
            authenticate("test") {
                setUpCallbackRoute("/validate")
            }
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get("/validate")
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("http://localhost:5173/", response.headers["Location"])

        val setCookieHeaders: List<String>? = response.headers.getAll(HttpHeaders.SetCookie)
        assertNotNull(setCookieHeaders, "Expected at least one Set-Cookie header.")

        val sessionCookieHeader = setCookieHeaders.find { it.startsWith("AuthenticatedSession=") }
        assertNotNull(sessionCookieHeader, "AuthenticatedSession cookie not found.")

        val cookieValueEncoded = URLDecoder.decode(sessionCookieHeader, "UTF-8")
            .substringAfter("AuthenticatedSession=")
            .substringBefore(";")

        val decoded = Json.decodeFromString<AuthenticatedSession>(cookieValueEncoded)
        assertFalse(decoded.admin ?: false)
    }
}

