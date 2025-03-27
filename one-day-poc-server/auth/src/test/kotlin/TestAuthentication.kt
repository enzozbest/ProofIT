import authentication.authentication.AuthenticatedSession
import authentication.authentication.Authenticators.configureJWTValidator
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import helpers.AuthenticationTestHelpers.configureBasicAuthentication
import helpers.AuthenticationTestHelpers.setupExternalServices
import helpers.AuthenticationTestHelpers.urlProvider
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import utils.json.PoCJSON.readJsonFile
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import kotlin.test.assertEquals

class TestAuthentication {
    private val rsaKeyPair = generateRSAKeyPair()
    private val rsaPrivateKey = rsaKeyPair.first
    private val rsaPublicKey = rsaKeyPair.second

    @Test
    fun `Test OAuth flow`() =
        testApplication {
            application {
                authentication {
                    configureBasicAuthentication()
                    configureJWTValidator(readJsonFile("cognito-test.json"))
                }
            }
            setupExternalServices()
            routing {
                authenticate("testAuth") {
                    get("/authenticate") {
                        call.respondRedirect(urlProvider["authorizeUrl"]!!.jsonPrimitive.content)
                    }
                }
            }
            client
                .get("/authenticate") {
                    header(HttpHeaders.Authorization, "Basic ${"test:password".encodeBase64()}")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    println(headers["Origin"])
                }
        }

    @Test
    fun `Test JWT Validator is set up and works with Authorization Header`() =
        testApplication {
            val mockJWKSUrl = "http://localhost:21001"
            setUpMockJWKSEndpoint(21001)

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

            val responseInvalidToken =
                client.get("test/protected") {
                    header(HttpHeaders.Authorization, "Bearer invalid_jwt_here")
                }
            assertEquals(HttpStatusCode.Unauthorized, responseInvalidToken.status)

            val responseWithToken =
                client.get("test/protected") {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken(21001)}")
                }
            assertEquals(HttpStatusCode.OK, responseWithToken.status)
        }

    @Test
    fun `Test JWT Validator is set up and works with AuthenticatedSession cookie`() =
        testApplication {
            val mockJWKSUrl = "http://localhost:21002"
            setUpMockJWKSEndpoint(21002)
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
            val responseWithoutCookie =
                client.get("test/protected") {
                    headers.clear()
                }
            assertEquals(HttpStatusCode.Unauthorized, responseWithoutCookie.status)
            val responseWithInvalidCookie =
                client.get("test/protected") {
                    cookie("AuthenticatedSession", "invalid_cookie_here")
                }
            assertEquals(HttpStatusCode.Unauthorized, responseWithInvalidCookie.status)
            val responseWithValidCookie =
                client.get("test/protected") {
                    val session = AuthenticatedSession("id", createValidToken(21002), false)
                    cookie(
                        "AuthenticatedSession",
                        Json.encodeToString(AuthenticatedSession.serializer(), session),
                    )
                }
            assertEquals(HttpStatusCode.OK, responseWithValidCookie.status)
            val responseWithInvalidToken =
                client.get("test/protected") {
                    cookie("AuthenticatedSession", createInvalidToken(21002))
                }
            assertEquals(HttpStatusCode.Unauthorized, responseWithInvalidToken.status)
        }

    private fun createValidToken(port: Int): String {
        val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
        return JWT
            .create()
            .withKeyId("test-key-id")
            .withIssuer("http://localhost:$port")
            .withClaim("sub", "test-user-id")
            .sign(algorithm)
    }

    private fun createInvalidToken(port: Int): String {
        val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
        return JWT
            .create()
            .withKeyId("test-key-id")
            .withIssuer("http://localhost:$port")
            .withClaim("sub", "")
            .sign(algorithm)
    }

    private fun createMockJWK(
        keyId: String,
        publicKey: RSAPublicKey,
    ): String {
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
                    call.respondText(
                        Json.encodeToString(
                            JsonObject.serializer(),
                            JsonObject(
                                mapOf(
                                    "keys" to
                                        JsonArray(
                                            listOf(
                                                Json.parseToJsonElement(
                                                    createMockJWK(
                                                        "test-key-id",
                                                        rsaPublicKey,
                                                    ),
                                                ),
                                            ),
                                        ),
                                ),
                            ),
                        ),
                        ContentType.Application.Json,
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

    private class MockJsonConfig(
        private val jwksUrl: String,
    ) {
        fun getJson(): JsonObject =
            buildJsonObject {
                put("jwtIssuer", JsonPrimitive(jwksUrl))
            }
    }
}
