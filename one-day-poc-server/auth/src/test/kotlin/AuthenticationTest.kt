import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kcl.seg.rtt.auth.*
import kcl.seg.rtt.utils.JSON.PoCJSON
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticationTest {

    private val jsonConfig: JsonObject =
        PoCJSON.readJsonFile("src/test/resources/cognito-test.json")
    private val urlProvider: JsonObject = jsonConfig["providerLookup"]!!.jsonObject


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
                call.respondRedirect(urlProvider["authorizeUrl"]!!.jsonPrimitive.content)
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
        setupExternalServices(jsonConfig)

        val myClient = createClient {
            followRedirects = false
        }

        val response = myClient.get(urlProvider["authorizeUrl"]!!.jsonPrimitive.content)
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

    private fun TestApplicationBuilder.setupExternalServices(urlProvider: JsonObject) {
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

    private val rsaKeyPair = generateRSAKeyPair()
    private val rsaPrivateKey = rsaKeyPair.first
    private val rsaPublicKey = rsaKeyPair.second

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test JWT Validator is set up and works with Authorization Header`() = testApplication {
        val mockJWKSUrl = "localhost:5000"
        this@JWTValidatorIntegrationTest.setUpMockJWKSEndpoint(5000)

        this.application {
            this@application.install(Authentication) {
                val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                configureJWTValidator(jwtConfig)
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

        val responseInvalidToken = client.get("test/protected") {
            header(HttpHeaders.Authorization, "Bearer invalid_jwt_here")
        }
        assertEquals(HttpStatusCode.Unauthorized, responseInvalidToken.status)

        val responseWithToken = client.get("test/protected") {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken(5000)}")
        }
        assertEquals(HttpStatusCode.OK, responseWithToken.status)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test JWT Validator is set up and works with AuthenticatedSession cookie`() = testApplication {
        val mockJWKSUrl = "localhost:6000"
        this@JWTValidatorIntegrationTest.setUpMockJWKSEndpoint(6000)

        this.application {
            this@application.install(Authentication) {
                val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                configureJWTValidator(jwtConfig)
            }

            routing {
                authenticate("jwt-verifier") {
                    get("/test/protected") {
                        call.respond(HttpStatusCode.OK, "Authorized!")
                    }
                }
            }
        }

        val responseWithoutCookie = client.get("test/protected") {
            headers.clear()
        }
        assertEquals(HttpStatusCode.Unauthorized, responseWithoutCookie.status)

        val responseWithInvalidCookie = client.get("test/protected") {
            cookie("AuthenticatedSession", "invalid_cookie_here")
        }
        assertEquals(HttpStatusCode.Unauthorized, responseWithInvalidCookie.status)

        val responseWithValidCookie = client.get("test/protected") {
            val session = AuthenticatedSession("id", createValidToken(6000), false)
            cookie("AuthenticatedSession", Json.encodeToString<AuthenticatedSession>(session))
        }
        assertEquals(HttpStatusCode.OK, responseWithValidCookie.status)

        val responseWithInvalidToken = client.get("test/protected") {
            cookie("AuthenticatedSession", createInvalidToken(6000))
        }
        assertEquals(HttpStatusCode.Unauthorized, responseWithInvalidToken.status)
    }


    private fun createValidToken(port: Int): String {
        val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
        return JWT.create()
            .withKeyId("test-key-id")
            .withIssuer("http://localhost:$port")
            .withClaim("email", "test@example.org")
            .sign(algorithm)
    }

    private fun createInvalidToken(port: Int): String {
        val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
        return JWT.create()
            .withKeyId("test-key-id")
            .withIssuer("http://localhost:$port")
            .withClaim("email", "")
            .sign(algorithm)
    }

    private fun createMockJWK(keyId: String, publicKey: RSAPublicKey): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()

        val modulus = encoder.encodeToString(publicKey.modulus.toByteArray())
        val exponent = encoder.encodeToString(publicKey.publicExponent.toByteArray())
        return """
            {
                "kty": "RSA",
                "kid": "$keyId",
                "typ": "JWT",
                "use": "sig",
                "alg": "RS256",
                "n": "$modulus",
                "e": "$exponent"
            }
        """.trimIndent()
    }

    private fun setUpMockJWKSEndpoint(port: Int) {
        embeddedServer(Netty, port = port) {
            routing {
                get("/.well-known/jwks.json") {
                    log.info("JWK Endpoint Called")
                    call.respondText(
                        Json.encodeToString(
                            JsonObject.serializer(),
                            JsonObject(
                                mapOf(
                                    "keys" to JsonArray(
                                        listOf(
                                            Json.parseToJsonElement(
                                                createMockJWK(
                                                    "test-key-id",
                                                    rsaPublicKey
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        ContentType.Application.Json
                    )
                }
            }
        }.start()
    }

    private fun generateRSAKeyPair(): Pair<RSAPrivateKey, RSAPublicKey> {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()
        return keyPair.private as RSAPrivateKey to keyPair.public as RSAPublicKey
    }

    private class MockJsonConfig(private val jwksUrl: String) {
        fun getJson(): JsonObject {
            return buildJsonObject {
                put("jwtIssuer", jwksUrl)
            }
        }
    }
}