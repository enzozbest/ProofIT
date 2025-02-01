package kcl.seg.rtt.auth

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.date.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

const val AUTHENTICATION_ROUTE: String = "/api/auth"
const val CALL_BACK_ROUTE: String = "/api/auth/callback"
const val LOG_OUT_ROUTE: String = "/api/logout"
const val JWT_VALIDATION_ROUTE: String = "/api/auth/me"
const val USER_INFO_ROUTE: String = "api/auth/userinfo"

/**
 * Configures the routes that will be used for authentication.
 */
fun Application.configureAuthenticationRoutes(authName: String = "Cognito") {
    routing {
        authenticate(authName) {
            setAuthenticationEndpoint(AUTHENTICATION_ROUTE)
            setUpCallbackRoute(CALL_BACK_ROUTE)
        }
        authenticate("jwt-verifier") {
            setUpJWTValidation(JWT_VALIDATION_ROUTE)
            setUpUserInfoRoute(USER_INFO_ROUTE)
        }
        setLogOutEndpoint(LOG_OUT_ROUTE)
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
 * Sets up the logout endpoint for the authentication process.
 */
private fun Route.setLogOutEndpoint(route: String) {
    get(route) {
        call.sessions.clear<AuthenticatedSession>()
        call.response.cookies.append(
            Cookie(
                "AuthenticatedSession",
                "",
                path = "/",
                httpOnly = true,
                secure = true,
                expires = GMTDate(0)
            )
        )
        call.respond(HttpStatusCode.OK)
    }
}

/**
 * Sets up JWT validation route
 */
fun Route.setUpJWTValidation(validationRoute: String) {
    get(validationRoute) {
        val sessionCookie =
            Json.decodeFromString<AuthenticatedSession>(call.request.cookies["AuthenticatedSession"] ?: "")
        call.respond(JWTValidationResponse(sessionCookie.userId, sessionCookie.admin))
    }
}

private fun Route.setUpUserInfoRoute(route: String) {
    get(route) {
        val sessionCookie =
            Json.decodeFromString<AuthenticatedSession>(call.request.cookies["AuthenticatedSession"] ?: "")
        val token = sessionCookie.token

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://cognito-idp.eu-west-2.amazonaws.com/")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/x-amz-json-1.1")
            .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.GetUser")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                return@get
            }
            val userinfo = generateUserInfo(response)
            call.respond(HttpStatusCode.OK, userinfo)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

/**
 * Sets up the callback route for the authentication process.
 */
fun Route.setUpCallbackRoute(route: String, redirectDomain: String = "http://localhost:5173") {
    get(route) {
        val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized) //Authentication failed, do not grant access.
            return@get
        }
        val token: String? = principal.extraParameters["id_token"]
        try {
            val decoded = JWT.decode(token ?: return@get call.respond(HttpStatusCode.Unauthorized))
            val userId: String =
                decoded.getClaim("sub").asString() ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val admin: Boolean =
                decoded.getClaim("cognito:groups").asList(String::class.java)?.contains("admin_users") ?: false

            call.sessions.set(AuthenticatedSession(userId, principal.accessToken, admin))
            val redirectUrl = call.request.queryParameters["redirect"] ?: "/"
            call.respondRedirect("$redirectDomain$redirectUrl")
        } catch (e: Exception) {
            return@get call.respond(HttpStatusCode.Unauthorized) //JWT token is invalid
        }
    }
}

