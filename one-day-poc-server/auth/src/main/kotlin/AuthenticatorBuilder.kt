package kcl.seg.rtt.auth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.json.JSONObject

/**
 * Configures the OAuth settings for the application.
 * Settings are loaded from a JSON file containing the relevant fields. In this case, the JSON file is expected to
 * contain the following fields, relevant to Amazon Cognito authentication: name, urlProvider, providerLookup.
 *
 * @param config The JSON object containing the configuration settings for the OAuth provider.
 */
fun AuthenticationConfig.configureOAuth(config: JSONObject) {
    val providerLookupData = config.getJSONObject("providerLookup")
    oauth(config.getString("name")) {
        urlProvider = { config.getString("urlProvider") }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = providerLookupData.getString("name"),
                authorizeUrl = providerLookupData.getString("authorizeUrl"),
                accessTokenUrl = providerLookupData.getString("accessTokenUrl"),
                clientId = providerLookupData.getString("clientId"),
                clientSecret = providerLookupData.getString("clientSecret"),
                defaultScopes = providerLookupData.getJSONArray("defaultScopes").let { jsonArray ->
                    List(jsonArray.length()) { index ->
                        jsonArray.getString(index)
                    }
                },
                requestMethod = HttpMethod.Post,
            )
        }
        client = HttpClient(CIO)
    }
}

fun AuthenticationConfig.configureJWTValidator(config: JSONObject) {
    jwt("jwt-verifier") {
        verifier(config.getString("verifier"))
        validate { credential ->
            if (credential.payload.getClaim("email").asString() != null)
                JWTPrincipal(credential.payload)
            else
                null
        }
    }
}