import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kcl.seg.rtt.auth.AUTHENTICATION_ROUTE
import kcl.seg.rtt.auth.CALL_BACK_ROUTE
import kcl.seg.rtt.auth.authModule
import kcl.seg.rtt.auth.configureJWTValidator
import kcl.seg.rtt.utils.JSON.readJsonFile
import org.json.JSONObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticationTest {

    private val jsonConfig: JSONObject =
        readJsonFile("src/test/resources/cognito-test.json")
    private val urlProvider: JSONObject = jsonConfig.getJSONObject("providerLookup")


    @Test
    fun `Test Authentication flow`() = testApplication {
        application {
            authentication {
                configureBasicAuthentication()
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
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider.getString("authorizeUrl")))
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
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider.getString("authorizeUrl")))
    }

    @Test
    fun `Test Authorize Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        setupExternalServices(jsonConfig)

        val myClient = createClient {
            followRedirects = false
        }

        val response = myClient.get(urlProvider.getString("authorizeUrl"))
        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Set up Basic authentication for testing purposes only
     */
    private fun AuthenticationConfig.configureBasicAuthentication() {
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

class JWTValidatorIntegrationTest {

    @Test
    fun `Test JWT Validator is set up and works`() = testApplication {
        this.application {
            this@application.install(Authentication) {
                configureJWTValidator(MockJsonConfig())
            }

            routing {
                authenticate("jwt-verifier") {
                    get("/test/protected") {
                        call.respond(HttpStatusCode.OK, "Authorized!")
                    }
                }
            }
        }

        val responseNoToken = client.get("test/protected")
        assertEquals(HttpStatusCode.Unauthorized, responseNoToken.status)

        val responseWithToken = client.get("test/protected") {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
        }
        assertEquals(HttpStatusCode.OK, responseWithToken.status)

        val responseInvalidToken = client.get("test/protected") {
            header(HttpHeaders.Authorization, "Bearer invalid_jwt_here")
        }
        assertEquals(HttpStatusCode.Unauthorized, responseInvalidToken.status)
    }

    private fun createValidToken(): String {
        val algorithm = Algorithm.HMAC256("secret") // Replace with your secret/key

        return JWT.create()
            .withIssuer("my-issuer")
            .withAudience("my-audience")
            .withClaim("email", "test@example.org")
            .sign(algorithm)
    }

    private class MockJsonConfig : JSONObject() {
        override fun getString(key: String?): String {
            return when (key) {
                "verifier" -> "mockVerifier"
                else -> error("Unexpected key: $key")
            }
        }
    }
}

//    @Test
//    fun `Test JWT Validator setup`() {
//        every { config.getString("verifier") } returns "someVerifier"
//        jwtValidator.configureJWTValidator(config)
//        verify { jwtValidator.jwt("jwt-verifier", any()) }
//
//        val mockConfig = mockk<JWTAuthenticationProvider.Config>(relaxed = true)
//        val configBlock = slot<JWTAuthenticationProvider.Config.() -> Unit>()
//        verify { jwtValidator.jwt("jwt-verifier", capture(configBlock)) }
//
//        val capturedBlock = configBlock.captured
//        capturedBlock.invoke(mockConfig)
//        verify { mockConfig.verifier("someVerifier") }
//    }
//}


//    @Test
//    fun `Test JWT Validator with valid email`() {
//        every {
//            config.getString("verifier")
//        } returns "someVerifier"
//
//        jwtValidator.configureJWTValidator(config)
//        val credential = createJWT("test@example.org")
//        val result = jwtValidator.jwt { validate { credential } }
//        println(result)
//        assertNotNull(result)
//    }
//
//    @Test
//    fun `Test JWT Validator with invalid email`() {
//        every {
//            config.getString("verifier")
//        } returns "someVerifier"
//
//        jwtValidator.configureJWTValidator(config)
//        val credential = createJWT("test")
//        val result = jwtValidator.jwt("jwt-verifier") { validate { credential } }
//        assertNull(result)
//    }
//
//    @Test
//    fun `Test JWT Validator with blank email`() {
//        every {
//            config.getString("verifier")
//        } returns "someVerifier"
//
//        jwtValidator.configureJWTValidator(config)
//        val credential = createJWT("")
//        val result = jwtValidator.jwt("jwt-verifier") { validate { credential } }
//        assertNull(result)
//    }
//
//    @Test
//    fun `Test JWT Validator with null email`() {
//        every {
//            config.getString("verifier")
//        } returns "someVerifier"
//
//        jwtValidator.configureJWTValidator(config)
//        val credential = createJWT(null)
//        val result = jwtValidator.jwt("jwt-verifier") { validate { credential } }
//        assertNull(result)
//    }
//
//    private fun createJWT(email: String?): JWTCredential {
//        val decodedJWT = mockk<DecodedJWT> {
//            every { getClaim("email").asString() } returns email
//        }
//        val credential = mockk<JWTCredential> {
//            every { payload } returns decodedJWT
//        }
//        return credential
//    }
//}

