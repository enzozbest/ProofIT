package chat

import authentication.authentication.Authenticators.configureJWTValidator
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

abstract class BaseAuthenticationServer {
    companion object {
        private var mockServer: EmbeddedServer<*, *>? = null
        private val rsaKeyPair = generateRSAKeyPair()
        private val rsaPrivateKey = rsaKeyPair.first
        private val rsaPublicKey = rsaKeyPair.second
        const val TEST_PORT = 9876

        init {
            startMockServer()
        }

        private fun startMockServer() {
            if (mockServer == null) {
                mockServer =
                    embeddedServer(Netty, port = TEST_PORT) {
                        routing {
                            get("/.well-known/jwks.json") {
                                call.respondText(
                                    createJWKSResponse(),
                                    ContentType.Application.Json,
                                )
                            }
                        }
                    }
                mockServer?.start(wait = false)
            }
        }

        private fun createJWKSResponse(): String =
            Json.Default.encodeToString(
                JsonObject.Companion.serializer(),
                JsonObject(
                    mapOf(
                        "keys" to
                            JsonArray(
                                listOf(
                                    Json.Default.parseToJsonElement(
                                        createMockJWK(
                                            "test-key-id",
                                            rsaPublicKey,
                                        ),
                                    ),
                                ),
                            ),
                    ),
                ),
            )

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

        private fun generateRSAKeyPair(): Pair<RSAPrivateKey, RSAPublicKey> {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()
            return keyPair.private as RSAPrivateKey to keyPair.public as RSAPublicKey
        }

        fun createValidToken(): String {
            val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
            return JWT
                .create()
                .withKeyId("test-key-id")
                .withIssuer("http://localhost:$TEST_PORT")
                .withClaim("sub", "test-user-id")
                .sign(algorithm)
        }
    }

    protected open fun Application.testModule() {
        routing {
            with(ChatEndpoints) {
                setChatRoutes()
                setJsonRoutes()
                setUploadRoutes()
            }
        }
    }

    protected fun ApplicationTestBuilder.setupTestApplication(moduleConfig: Application.() -> Unit = { testModule() }) {
        val mockJWKSUrl = "http://localhost:$TEST_PORT"

        application {
            install(Authentication.Companion) {
                val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                configureJWTValidator(jwtConfig)
            }
            install(ContentNegotiation) {
                json()
            }
            moduleConfig()
        }
    }

    private class MockJsonConfig(
        private val jwksUrl: String,
    ) {
        fun getJson(): JsonObject =
            buildJsonObject {
                put("jwtIssuer", jwksUrl)
            }
    }
}
