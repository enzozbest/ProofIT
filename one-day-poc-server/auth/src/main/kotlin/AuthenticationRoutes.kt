package kcl.seg.rtt.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures the routes that will be used for authentication.
 */

val SIGN_UP_ROUTE: String = "/api/signup"
val LOG_IN_ROUTE: String = "/api/login"
val CALL_BACK_ROUTE: String = "/api/callback"

fun Application.configureAuthenticationRoutes(authName: String = "Cognito") {
    routing {
        authenticate(authName) {
            get(SIGN_UP_ROUTE) {
                call.respondRedirect("/authenticate")
            }
            get(LOG_IN_ROUTE) {
                call.respondRedirect("/authenticate")
            }
            get(CALL_BACK_ROUTE) {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                if (principal != null) {
                    call.respondText("Login successful!", status = HttpStatusCode.OK)
                } else {
                    call.respondText("Login failed!", status = HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}