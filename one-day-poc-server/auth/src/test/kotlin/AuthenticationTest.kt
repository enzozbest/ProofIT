import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kcl.seg.rtt.auth.AUTHENTICATION_ROUTE
import kcl.seg.rtt.auth.CALL_BACK_ROUTE
import kcl.seg.rtt.auth.UserSession
import kcl.seg.rtt.auth.authModule
import kcl.seg.rtt.utils.JSON.readJsonFile
import org.json.JSONObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticationTest {

    private val json_config: JSONObject =
        readJsonFile("src/test/resources/cognito-test.json")
    private val urlProvider: JSONObject = json_config.getJSONObject("providerLookup")

    @Test
    fun testUserSessionClass() {
        val userSession = UserSession("userId", "token", true)

        assertEquals("userId", userSession.userId)
        assertEquals("token", userSession.token)
        assertTrue { userSession.admin }
    }

    @Test
    fun testAuthenticationFlow() = testApplication {
        application {
            authentication {
                configureTestBasic()
            }
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }

        setupExternalServices(urlProvider)

        routing {
            get("/authenticate") {
                call.respondRedirect(urlProvider.getString("authorizeUrl"))
            }
        }

        client.get(AUTHENTICATION_ROUTE) {
            header(HttpHeaders.Authorization, "Basic ${"test:password".encodeBase64()}")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            println(headers["Origin"])
        }
    }

    @Test
    fun testAuthenticationRouteExists() = testApplication {
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
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider.getString("authorizeUrl")))
    }

    @Test
    fun testCallbackRouteExists() = testApplication {
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
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider.getString("authorizeUrl")))
    }

    @Test
    fun testAuthorizeRouteExists() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        setupExternalServices(json_config)

        val myClient = createClient {
            followRedirects = false
        }

        val response = myClient.get(urlProvider.getString("authorizeUrl"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Set up Basic authentication for testing purposes only
     */
    private fun AuthenticationConfig.configureTestBasic() {
        basic("testAuth") {
            validate { credentials ->
                if (credentials.name == "test" && credentials.password == "password") {
                    UserIdPrincipal(credentials.name)
                } else null
            }
        }
    }

    private fun TestApplicationBuilder.setupExternalServices(urlProvider: JSONObject) {
        externalServices {
            hosts("http://example.com:2000") {
                routing {
                    install(ContentNegotiation) {
                        json()
                    }
                    get("/authorize") {
                        val mockPrincipalJson = """
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
}
