package kcl.seg.rtt.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

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
 * @param config The JSON object containing the configuration settings for the JWT validator.
 */
fun AuthenticationConfig.configureJWTValidator(config: JsonObject) {
    jwt("jwt-verifier") {
        val issuer = config["jwtIssuer"]!!.jsonPrimitive.content
        val jwkProvider = JwkProviderBuilder("http://$issuer")
            .cached(10, 1, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
        verifier(jwkProvider, "http://$issuer") {
            acceptLeeway(10)
        }
        validate { credential ->
            if (credential.payload.getClaim("email").asString() != null)
                JWTPrincipal(credential.payload)
            else
                null
        }
    }
}