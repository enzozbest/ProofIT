package kcl.seg.rtt.auth

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

val AUTHENTICATION_ROUTE: String = "/api/auth"
val CALL_BACK_ROUTE: String = "/api/auth/callback"
val LOG_OUT_ROUTE: String = "/api/logout"


/**
 * Configures the routes that will be used for authentication.
 */
fun Application.configureAuthenticationRoutes(authName: String = "Cognito") {
    setupSessions()

    routing {
        authenticate(authName) {
            setAuthenticationEndpoint(AUTHENTICATION_ROUTE)
            setLogOutEndpoint(LOG_OUT_ROUTE)
            setUpCallbackRoute(CALL_BACK_ROUTE)
        }
    }
}

/**
 * Sets up the authentication endpoint for the authentication process.
 */
private fun Route.setAuthenticationEndpoint(route: String) {
    get(route) {
        call.respondRedirect("/authenticate")
    }
}

/**
 * Sets up the callback route for the authentication process.
 */
private fun Route.setUpCallbackRoute(route: String) {
    get(route) {
        val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()

        if (principal == null)
            call.respond(HttpStatusCode.Unauthorized) //Authentication failed, do not grant access.

        val token: String? = principal!!.extraParameters["id_token"]
        val decoded = JWT.decode(token)
        val userId: String = decoded.getClaim("sub").asString()
        val admin: Boolean? =
            decoded.getClaim("cognito:groups")?.asList(String::class.java)?.contains("admin_users")
        call.sessions.set(AuthenticatedSession(userId, principal.accessToken, admin))
        call.respondRedirect("http://localhost:5173")
    }
}

/**
 * Sets up the logout endpoint for the authentication process.
 */
private fun Route.setLogOutEndpoint(route: String) {
    post(route) {
        call.sessions.clear<UserSession>()
        call.respond(HttpStatusCode.OK)
    }
}

/**
 * Sets up the sessions for the application.
 */
private fun Application.setupSessions() {
    install(Sessions) {
        cookie<GuestSession>("GuestSession") {
            cookie.maxAgeInSeconds = 3600
        }
        cookie<AuthenticatedSession>("AuthenticatedSession") {
            cookie.maxAgeInSeconds = 3600
        }
    }
}

