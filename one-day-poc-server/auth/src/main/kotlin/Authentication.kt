package kcl.seg.rtt.auth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import kcl.seg.rtt.utils.JSON.readJsonFile
import org.json.JSONObject

/**
 * Configures the authentication settings for the application and the routes that will be used for authentication.
 *
 * This function is called automatically by Ktor when the application is started (declared as a module entry point
 * in application.conf).
 */
fun Application.authModule(
    configFilePath: String = "auth/src/main/resources/cognito.json",
    authName: String = "Cognito"
) {
    configureAuthentication(configFilePath)
    configureAuthenticationRoutes(authName = authName)
}


/**
 * Configures the authentication settings for the application.
 */
private fun Application.configureAuthentication(configFilePath: String) {
    val config = readJsonFile(configFilePath)
    try {
        install(Authentication) {
            configureOAuth(config)
        }
    } catch (e: DuplicatePluginException) {
        print("")
    }
}

/**
 * Configure OAuth 2.0 for secure authentication
 */
private fun AuthenticationConfig.configureOAuth(config: JSONObject) {
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




