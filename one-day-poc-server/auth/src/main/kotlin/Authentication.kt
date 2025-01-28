package kcl.seg.rtt.auth

import io.ktor.server.application.*
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.client.engine.cio.*
import kcl.seg.rtt.utils.JSON.readJsonFile

/**
 * Configures the authentication settings for the application and the routes that will be used for authentication.
 *
 * This function is called automatically by Ktor when the application is started (declared as a module entry point
 * in application.conf).
 */
fun Application.module(){
    configureAuthentication()
    configureAuthenticationRoutes()
}

/**
 * Configures the authentication settings for the application.
 */
private fun Application.configureAuthentication() {
    val authConfig = readJsonFile("auth/src/main/resources/cognito.json")
    val providerLookupData = authConfig.getJSONObject("providerLookup")

    println(providerLookupData.getString("accessTokenUrl"))
    install(Authentication) {
        oauth("cognito") {
            urlProvider = { authConfig.getString("urlProvider") }
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
}