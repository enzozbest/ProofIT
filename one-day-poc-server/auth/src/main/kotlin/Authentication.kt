package kcl.seg.rtt.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import kcl.seg.rtt.utils.JSON.PoCJSON

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
    configureSessions()
    configureAuthenticationRoutes(authName = authName)
}

/**
 * Configures the authentication settings for the application.
 */
private fun Application.configureAuthentication(configFilePath: String) {
    val config = PoCJSON.readJsonFile(configFilePath)
    try {
        install(Authentication) {
            configureOAuth(config)
            configureJWTValidator(config)
        }
    } catch (e: DuplicatePluginException) {
        print("")
    }
}

/**
 * Sets up the sessions for the application.
 */
private fun Application.configureSessions() {
    install(Sessions) {
        cookie<AuthenticatedSession>("AuthenticatedSession") {
            cookie.maxAgeInSeconds = 3600
            cookie.secure = true
            cookie.httpOnly = true
        }
    }
}
