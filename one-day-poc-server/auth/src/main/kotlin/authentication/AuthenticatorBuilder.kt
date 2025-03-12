package authentication

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.oauth
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

internal object JWTConstants {
    const val LEEWAY: Long = 10
    const val JWK_PROVIDER_CACHE_SIZE: Long = 10
    const val JWK_PROVIDER_EXPIRES_IN: Long = 24
    const val JWK_PROVIDER_BUCKET_SIZE: Long = 10
}

internal object Authenticators {
    private lateinit var jwkProvider: JwkProvider

    /**
     * Configures the OAuth settings for the application.
     * Settings are loaded from a JSON file containing the relevant fields. In this case, the JSON file is expected to
     * contain the following fields, relevant to Amazon Cognito authentication: name, urlProvider, providerLookup.
     *
     * @param config The JSON object containing the configuration settings for the OAuth provider.
     */
    internal fun AuthenticationConfig.configureOAuth(config: JsonObject) {
        val providerLookupData = config["providerLookup"]!!.jsonObject
        oauth(config["name"]!!.jsonPrimitive.content) {
            urlProvider = { config["urlProvider"]!!.jsonPrimitive.content }
            providerLookup = {
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
            client = HttpClient(CIO)
        }
    }

    /**
     * Configures the JWT settings for the application. Settings are loaded from a JSON file containing
     * the relevant fields. In this case, the JSON file is expected to contain the jwtIssuer field.
     * This is a JWKS Domain from which the application will retrieve the signing key for the JWT.
     * For authorisation, a JWT is expected either in the Authorization header or in am AuthenticatedSession cookie
     *
     * @param config The JSON object containing the configuration settings for the JWT validator.
     */
    internal fun AuthenticationConfig.configureJWTValidator(config: JsonObject) {
        val issuer = config["jwtIssuer"]!!.jsonPrimitive.content
        jwkProvider =
            JwkProviderBuilder(issuer)
                .cached(JWTConstants.JWK_PROVIDER_CACHE_SIZE, JWTConstants.JWK_PROVIDER_EXPIRES_IN, TimeUnit.HOURS)
                .rateLimited(JWTConstants.JWK_PROVIDER_BUCKET_SIZE, 1, TimeUnit.MINUTES)
                .build()

        generateVerifier(jwkProvider, issuer)
    }
    /* Line 61 above is marked as partially covered because of the function it calls.
     * That function uses an inline lambda, which most coverage tools struggle to appropriately
     * judge in regard to execution status.
     */

    private fun AuthenticationConfig.generateVerifier(
        jwkProvider: JwkProvider,
        issuer: String,
    ) {
        jwt("jwt-verifier") {
            authHeader { call ->
                val sessionCookie =
                    call.request.cookies["AuthenticatedSession"]
                        ?: return@authHeader call.request.headers["Authorization"]?.let { parseAuthorizationHeader(it) }

                return@authHeader try {
                    val session = Json.decodeFromString<AuthenticatedSession>(sessionCookie)
                    parseAuthorizationHeader("Bearer ${session.token}")
                } catch (e: SerializationException) {
                    null
                }
            }
            verifier(jwkProvider, issuer) {
                acceptLeeway(JWTConstants.LEEWAY)
            }
            validate { credential ->
                credential.payload
                    .getClaim("sub")
                    .asString()
                    ?.let { JWTPrincipal(credential.payload) }
            }
        }
    }
}
/*
* Line above is marked as partially covered because of a known issue with coverage tools and some cases of Kotlin's
* inlined lambdas. The line is fully covered by the tests, but most coverages tools will not mark it as such.
* */
