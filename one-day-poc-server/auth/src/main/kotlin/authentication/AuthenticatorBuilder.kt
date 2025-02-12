package kcl.seg.rtt.auth.authentication

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import java.util.concurrent.TimeUnit

object Authenticators {
    private lateinit var jwkProvider: JwkProvider

    /**
     * Configures the OAuth settings for the application.
     * Settings are loaded from a JSON file containing the relevant fields. In this case, the JSON file is expected to
     * contain the following fields, relevant to Amazon Cognito authentication: name, urlProvider, providerLookup.
     *
     * @param config The JSON object containing the configuration settings for the OAuth provider.
     */
    fun AuthenticationConfig.configureOAuth(config: JsonObject) {
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
     * Configures the JWT settings for the application.
     * Settings are loaded from a JSON file containing the relevant fields. In this case, the JSON file is expected to
     * contain the jwtIssuer field. This is a JWKS Domain from which the application will retrieve the signing key for the JWT.
     *
     * For authorisation, a JWT is expected either in the Authorization header or in am AuthenticatedSession cookie
     *
     * @param config The JSON object containing the configuration settings for the JWT validator.
     */
    fun AuthenticationConfig.configureJWTValidator(config: JsonObject) {
        val issuer = config["jwtIssuer"]!!.jsonPrimitive.content
        jwkProvider =
            JwkProviderBuilder(issuer)
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()

        generateVerifier(jwkProvider, issuer)
    }
    /* Line 61 above is marked as partially covered because of the function it calls. That function uses an inline lambda,
     * which most coverage tools struggle to appropriately judge in regard to execution status.
     * */

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
                acceptLeeway(10)
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
* Line 91 above is marked as partially covered because of a known issue with coverage tools and some cases of Kotlin's
* inlined lambdas. The line is fully covered by the tests, but most coverages tools will not mark it as such.
* */
