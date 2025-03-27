import authentication.authentication.AuthenticatedSession
import authentication.authentication.Authenticators.configureJWTValidator
import authentication.authentication.Authenticators.configureOAuth
import authentication.authentication.configureAuthenticators
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import kotlin.test.assertEquals

class TestAuthenticators {
    private val rsaKeyPair = generateRSAKeyPair()
    private val rsaPrivateKey = rsaKeyPair.first
    private val rsaPublicKey = rsaKeyPair.second

    @Test
    fun `Test configureOAuth method`() =
        testApplication {
            application {
                install(Authentication) {
                    val oauthConfig = createOAuthConfig()
                    configureOAuth(oauthConfig)
                }

                routing {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test")
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test OAuthServerSettings creation`() {
        val oauthConfig = createOAuthConfig()
        val providerLookupData = oauthConfig["providerLookup"]!!.jsonObject

        val settings =
            OAuthServerSettings.OAuth2ServerSettings(
                name = providerLookupData["name"]!!.jsonPrimitive.content,
                authorizeUrl = providerLookupData["authorizeUrl"]!!.jsonPrimitive.content,
                accessTokenUrl = providerLookupData["accessTokenUrl"]!!.jsonPrimitive.content,
                clientId = providerLookupData["clientId"]!!.jsonPrimitive.content,
                clientSecret = providerLookupData["clientSecret"]!!.jsonPrimitive.content,
                defaultScopes = providerLookupData["defaultScopes"]!!.jsonArray.map { it.jsonPrimitive.content },
                requestMethod = HttpMethod.Post,
            )

        assertEquals("test-provider", settings.name)
        assertEquals("http://localhost:20003/oauth/authorize", settings.authorizeUrl)
        assertEquals("http://localhost:20003/oauth/token", settings.accessTokenUrl)
        assertEquals("test-client-id", settings.clientId)
        assertEquals("test-client-secret", settings.clientSecret)
        assertEquals(listOf("email", "profile"), settings.defaultScopes)
        assertEquals(HttpMethod.Post, settings.requestMethod)
    }

    @Test
    fun `Test OAuthServerSettings creation with missing fields throws exception`() {
        val missingNameConfig = createOAuthConfigWithMissingField("name")
        val missingAuthorizeUrlConfig = createOAuthConfigWithMissingField("authorizeUrl")
        val missingAccessTokenUrlConfig = createOAuthConfigWithMissingField("accessTokenUrl")
        val missingClientIdConfig = createOAuthConfigWithMissingField("clientId")
        val missingClientSecretConfig = createOAuthConfigWithMissingField("clientSecret")
        val missingDefaultScopesConfig = createOAuthConfigWithMissingField("defaultScopes")

        val configs =
            listOf(
                missingNameConfig,
                missingAuthorizeUrlConfig,
                missingAccessTokenUrlConfig,
                missingClientIdConfig,
                missingClientSecretConfig,
                missingDefaultScopesConfig,
            )

        for (config in configs) {
            val providerLookupData = config["providerLookup"]!!.jsonObject

            kotlin.test.assertFailsWith<NullPointerException> {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = providerLookupData["name"]!!.jsonPrimitive.content,
                    authorizeUrl = providerLookupData["authorizeUrl"]!!.jsonPrimitive.content,
                    accessTokenUrl = providerLookupData["accessTokenUrl"]!!.jsonPrimitive.content,
                    clientId = providerLookupData["clientId"]!!.jsonPrimitive.content,
                    clientSecret = providerLookupData["clientSecret"]!!.jsonPrimitive.content,
                    defaultScopes = providerLookupData["defaultScopes"]!!.jsonArray.map { it.jsonPrimitive.content },
                    requestMethod = HttpMethod.Post,
                )
            }
        }
    }

    @Test
    fun `Test OAuthServerSettings creation with malformed fields throws exception`() {
        val malformedScopesConfig =
            buildJsonObject {
                put("name", JsonPrimitive("test-oauth"))
                put("urlProvider", JsonPrimitive("http://localhost:9000/oauth"))
                put(
                    "providerLookup",
                    buildJsonObject {
                        put("name", JsonPrimitive("test-provider"))
                        put("authorizeUrl", JsonPrimitive("http://localhost:9000/oauth/authorize"))
                        put("accessTokenUrl", JsonPrimitive("http://localhost:9000/oauth/token"))
                        put("clientId", JsonPrimitive("test-client-id"))
                        put("clientSecret", JsonPrimitive("test-client-secret"))
                        put("defaultScopes", JsonPrimitive("not-an-array"))
                    },
                )
            }

        val providerLookupData = malformedScopesConfig["providerLookup"]!!.jsonObject

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            OAuthServerSettings.OAuth2ServerSettings(
                name = providerLookupData["name"]!!.jsonPrimitive.content,
                authorizeUrl = providerLookupData["authorizeUrl"]!!.jsonPrimitive.content,
                accessTokenUrl = providerLookupData["accessTokenUrl"]!!.jsonPrimitive.content,
                clientId = providerLookupData["clientId"]!!.jsonPrimitive.content,
                clientSecret = providerLookupData["clientSecret"]!!.jsonPrimitive.content,
                defaultScopes = providerLookupData["defaultScopes"]!!.jsonArray.map { it.jsonPrimitive.content },
                requestMethod = HttpMethod.Post,
            )
        }
    }

    @Test
    fun `Test OAuth server settings creation directly`() {
        val oauthConfig = createOAuthConfig()
        val providerLookupData = oauthConfig["providerLookup"]!!.jsonObject

        val settings =
            OAuthServerSettings.OAuth2ServerSettings(
                name = providerLookupData["name"]!!.jsonPrimitive.content,
                authorizeUrl = providerLookupData["authorizeUrl"]!!.jsonPrimitive.content,
                accessTokenUrl = providerLookupData["accessTokenUrl"]!!.jsonPrimitive.content,
                clientId = providerLookupData["clientId"]!!.jsonPrimitive.content,
                clientSecret = providerLookupData["clientSecret"]!!.jsonPrimitive.content,
                defaultScopes = providerLookupData["defaultScopes"]!!.jsonArray.map { it.jsonPrimitive.content },
                requestMethod = HttpMethod.Post,
            )

        assertEquals("test-provider", settings.name)
        assertEquals("http://localhost:20003/oauth/authorize", settings.authorizeUrl)
        assertEquals("http://localhost:20003/oauth/token", settings.accessTokenUrl)
        assertEquals("test-client-id", settings.clientId)
        assertEquals("test-client-secret", settings.clientSecret)
        assertEquals(listOf("email", "profile"), settings.defaultScopes)
        assertEquals(HttpMethod.Post, settings.requestMethod)
    }

    @Test
    fun `Test JWT validation with malformed AuthenticatedSession cookie`() =
        testApplication {
            val mockJWKSUrl = "http://localhost:20001"
            setUpMockJWKSEndpoint(20001)

            application {
                install(Authentication) {
                    val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                    configureJWTValidator(jwtConfig)
                }
                routing {
                    authenticate("jwt-verifier") {
                        get("/protected") {
                            call.respond(HttpStatusCode.OK, "Authorized!")
                        }
                    }
                }
            }

            val responseWithMalformedCookie =
                client.get("/protected") {
                    cookie("AuthenticatedSession", "this-is-not-valid-json")
                }
            assertEquals(HttpStatusCode.Unauthorized, responseWithMalformedCookie.status)
        }

    @Test
    fun `Test JWT validation with both cookie and Authorization header missing`() =
        testApplication {
            val mockJWKSUrl = "http://localhost:20002"
            setUpMockJWKSEndpoint(20002)

            application {
                install(Authentication) {
                    val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                    configureJWTValidator(jwtConfig)
                }
                routing {
                    authenticate("jwt-verifier") {
                        get("/protected") {
                            call.respond(HttpStatusCode.OK, "Authorized!")
                        }
                    }
                }
            }

            val responseWithNeitherCookieNorHeader =
                client.get("/protected") {
                    headers.clear()
                }
            assertEquals(HttpStatusCode.Unauthorized, responseWithNeitherCookieNorHeader.status)
        }

    private fun createOAuthConfig(baseUrl: String = "http://localhost:20003"): JsonObject =
        buildJsonObject {
            put("name", JsonPrimitive("test-oauth"))
            put("urlProvider", JsonPrimitive("$baseUrl/oauth"))
            put(
                "providerLookup",
                buildJsonObject {
                    put("name", JsonPrimitive("test-provider"))
                    put("authorizeUrl", JsonPrimitive("$baseUrl/oauth/authorize"))
                    put("accessTokenUrl", JsonPrimitive("$baseUrl/oauth/token"))
                    put("clientId", JsonPrimitive("test-client-id"))
                    put("clientSecret", JsonPrimitive("test-client-secret"))
                    put("defaultScopes", JsonArray(listOf(JsonPrimitive("email"), JsonPrimitive("profile"))))
                },
            )
        }

    private fun createOAuthConfigWithMissingField(
        fieldToRemove: String,
        baseUrl: String = "http://localhost:9000",
    ): JsonObject {
        val config =
            buildJsonObject {
                put("name", JsonPrimitive("test-oauth"))
                put("urlProvider", JsonPrimitive("$baseUrl/oauth"))
                put(
                    "providerLookup",
                    buildJsonObject {
                        if (fieldToRemove != "name") put("name", JsonPrimitive("test-provider"))
                        if (fieldToRemove != "authorizeUrl") {
                            put(
                                "authorizeUrl",
                                JsonPrimitive("$baseUrl/oauth/authorize"),
                            )
                        }
                        if (fieldToRemove != "accessTokenUrl") {
                            put(
                                "accessTokenUrl",
                                JsonPrimitive("$baseUrl/oauth/token"),
                            )
                        }
                        if (fieldToRemove != "clientId") put("clientId", JsonPrimitive("test-client-id"))
                        if (fieldToRemove != "clientSecret") put("clientSecret", JsonPrimitive("test-client-secret"))
                        if (fieldToRemove != "defaultScopes") {
                            put(
                                "defaultScopes",
                                JsonArray(listOf(JsonPrimitive("email"), JsonPrimitive("profile"))),
                            )
                        }
                    },
                )
            }
        return config
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

    private fun createTokenWithoutSubClaim(port: Int): String {
        val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
        return JWT
            .create()
            .withKeyId("test-key-id")
            .withIssuer("http://localhost:$port")
            .sign(algorithm)
    }

    private fun createTokenWithNullSubClaim(port: Int): String {
        val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
        return JWT
            .create()
            .withKeyId("test-key-id")
            .withIssuer("http://localhost:$port")
            .withClaim("not-sub", "some-value") // Add a different claim instead of "sub"
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

    @Test
    fun `Test configureAuthenticators method`() =
        testApplication {
            application {
                install(Authentication) {
                    val combinedConfig =
                        buildJsonObject {
                            put("name", JsonPrimitive("test-oauth"))
                            put("urlProvider", JsonPrimitive("http://example.com/oauth"))
                            put(
                                "providerLookup",
                                buildJsonObject {
                                    put("name", JsonPrimitive("test-provider"))
                                    put("authorizeUrl", JsonPrimitive("http://example.com/oauth/authorize"))
                                    put("accessTokenUrl", JsonPrimitive("http://example.com/oauth/token"))
                                    put("clientId", JsonPrimitive("test-client-id"))
                                    put("clientSecret", JsonPrimitive("test-client-secret"))
                                    put(
                                        "defaultScopes",
                                        JsonArray(listOf(JsonPrimitive("email"), JsonPrimitive("profile"))),
                                    )
                                },
                            )
                            put("jwtIssuer", JsonPrimitive("http://example.com"))
                        }
                    configureAuthenticators(combinedConfig)
                }

                routing {
                    get("/test") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            val response = client.get("/test")
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test JWT validation with valid AuthenticatedSession cookie`() =
        testApplication {
            val port = 20004
            val mockJWKSUrl = "http://localhost:$port"
            setUpMockJWKSEndpoint(port)

            application {
                install(Authentication) {
                    val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                    configureJWTValidator(jwtConfig)
                }
                routing {
                    authenticate("jwt-verifier") {
                        get("/protected") {
                            call.respond(HttpStatusCode.OK, "Authorized!")
                        }
                    }
                }
            }

            val validToken = createValidToken(port)

            val validSession = AuthenticatedSession(userId = "test-user-id", token = validToken, admin = false)
            val serializedSession = Json.encodeToString(AuthenticatedSession.serializer(), validSession)

            val responseWithValidCookie =
                client.get("/protected") {
                    cookie("AuthenticatedSession", serializedSession)
                }
            assertEquals(HttpStatusCode.OK, responseWithValidCookie.status)
        }

    @Test
    fun `Test JWT validation with valid Authorization header`() =
        testApplication {
            val port = 20005
            val mockJWKSUrl = "http://localhost:$port"
            setUpMockJWKSEndpoint(port)

            application {
                install(Authentication) {
                    val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                    configureJWTValidator(jwtConfig)
                }
                routing {
                    authenticate("jwt-verifier") {
                        get("/protected") {
                            call.respond(HttpStatusCode.OK, "Authorized!")
                        }
                    }
                }
            }

            val validToken = createValidToken(port)

            val responseWithValidHeader =
                client.get("/protected") {
                    header(HttpHeaders.Authorization, "Bearer $validToken")
                }
            assertEquals(HttpStatusCode.OK, responseWithValidHeader.status)
        }

    @Test
    fun `Test JWT validation with invalid sub claim`() =
        testApplication {
            val port = 20006
            val mockJWKSUrl = "http://localhost:$port"
            setUpMockJWKSEndpoint(port)

            application {
                install(Authentication) {
                    val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                    configureJWTValidator(jwtConfig)
                }
                routing {
                    authenticate("jwt-verifier") {
                        get("/protected") {
                            call.respond(HttpStatusCode.OK, "Authorized!")
                        }
                    }
                }
            }

            val invalidToken = createTokenWithoutSubClaim(port)

            val responseWithInvalidToken =
                client.get("/protected") {
                    header(HttpHeaders.Authorization, "Bearer $invalidToken")
                }
            assertEquals(HttpStatusCode.Unauthorized, responseWithInvalidToken.status)
        }

    @Test
    fun `Test JWT validation with null sub claim`() =
        testApplication {
            val port = 20007
            val mockJWKSUrl = "http://localhost:$port"
            setUpMockJWKSEndpoint(port)

            application {
                install(Authentication) {
                    val jwtConfig = MockJsonConfig(mockJWKSUrl).getJson()
                    configureJWTValidator(jwtConfig)
                }
                routing {
                    authenticate("jwt-verifier") {
                        get("/protected") {
                            call.respond(HttpStatusCode.OK, "Authorized!")
                        }
                    }
                }
            }

            val invalidToken = createTokenWithNullSubClaim(port)

            val responseWithInvalidToken =
                client.get("/protected") {
                    header(HttpHeaders.Authorization, "Bearer $invalidToken")
                }
            assertEquals(HttpStatusCode.Unauthorized, responseWithInvalidToken.status)
        }
}
