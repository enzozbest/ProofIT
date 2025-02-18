package kcl.seg.rtt.auth

import io.ktor.server.application.Application
import io.ktor.server.application.DuplicatePluginException
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kcl.seg.rtt.auth.authentication.AuthenticatedSession
import kcl.seg.rtt.auth.authentication.Authenticators.configureJWTValidator
import kcl.seg.rtt.auth.authentication.Authenticators.configureOAuth
import kcl.seg.rtt.auth.authentication.configureAuthenticationRoutes
import kcl.seg.rtt.utils.json.PoCJSON
import kotlin.collections.set

object AuthenticationConstants {
    const val DEFAULT_EXPIRATION_SECONDS = 600L
    const val CONFIGURATION_FILE_PATH = "auth/src/main/resources/cognito.json"
    const val DEFAULT_AUTHENTICATOR = "Cognito"
}

/**
 * Configures the authentication settings for the application and the routes that will be used for authentication.
 *
 * This function is called automatically by Ktor when the application is started (declared as a module entry point
 * in application.conf).
 */
fun Application.authModule(
    configFilePath: String = AuthenticationConstants.CONFIGURATION_FILE_PATH,
    authName: String = AuthenticationConstants.DEFAULT_AUTHENTICATOR,
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
            cookie.maxAgeInSeconds = AuthenticationConstants.DEFAULT_EXPIRATION_SECONDS
            cookie.secure = true
            cookie.httpOnly = true
            cookie.path = "/"
            cookie.extensions["SameSite"] = "None"
        }
    }
}
