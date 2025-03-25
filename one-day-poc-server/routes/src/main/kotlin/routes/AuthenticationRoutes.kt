package routes

import authentication.authentication.setProtectedJWTRoutes
import authentication.authentication.setProtectedOAuthRoutes
import authentication.authentication.setUnprotectedRoutes
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

object AuthenticationRoutes {
    private const val DEFAULT_AUTHENTICATOR = "Cognito"

    fun Application.configureAuthenticationRoutes() =
        routing {
            authenticate(DEFAULT_AUTHENTICATOR) {
                setProtectedOAuthRoutes()
            }
            authenticate("jwt-verifier") {
                setProtectedJWTRoutes()
            }
            setUnprotectedRoutes()
        }
}
