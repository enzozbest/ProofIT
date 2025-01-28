package kcl.seg.rtt.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures the routes that will be used for authentication.
 */
fun Application.configureAuthenticationRoutes(){
    routing{
        authenticate("cognito") {
            get("/api/signup"){
                call.respondRedirect("/authenticate")
            }
            get("/api/login"){
                call.respondRedirect("/authenticate")
            }
            get("/api/callback"){
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                if (principal != null){
                    call.respondText("Login successful!", status = HttpStatusCode.OK)
                } else {
                    call.respondText("Login failed!", status = HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}