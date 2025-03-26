package authentication.authentication

import authentication.authentication.Authenticators.configureJWTValidator
import authentication.authentication.Authenticators.configureOAuth
import io.ktor.server.auth.AuthenticationConfig
import kotlinx.serialization.json.JsonObject

/**
 * Configures the authentication settings for the application and the routes that will be used for authentication.
 *
 * This function is called automatically by Ktor when the application is started (declared as a module entry point
 * in application.conf).
 */

fun AuthenticationConfig.configureAuthenticators(config: JsonObject) {
    configureOAuth(config)
    configureJWTValidator(config)
}
