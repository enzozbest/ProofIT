package kcl.seg.rtt.auth

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.date.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val AUTHENTICATION_ROUTE: String = "/api/auth"
const val CALL_BACK_ROUTE: String = "/api/auth/callback"
const val LOG_OUT_ROUTE: String = "/api/auth/logout"
const val JWT_VALIDATION_ROUTE: String = "/api/auth/check"
const val USER_INFO_ROUTE: String = "api/auth/me"

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
    post(route) {
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

/**
 * Sets up a route to retrieve user information from the relevant authentication provider.
 */
fun Route.setUpUserInfoRoute(
    route: String,
    verifierUrl: String = "https://cognito-idp.eu-west-2.amazonaws.com/",
    contentType: String = "application/x-amz-json-1.1",
    amzTarget: Boolean = true,
    amzApi: String = "AWSCognitoIdentityProviderService.GetUser"
) {
    get(route) {
        val sessionJson = call.request.cookies["AuthenticatedSession"] ?: return@get call.respond(
            HttpStatusCode.Unauthorized,
            "Missing authentication cookie"
        )

        val token = try {
            Json.decodeFromString<AuthenticatedSession>(sessionJson).token
        } catch (e: Exception) {
            return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token format")
        }

        val response = buildUserInfoRequest(token, verifierUrl, contentType, amzTarget, amzApi).sendRequest()
        if (!response.isSuccessful) {
            return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")
        }

        val userInfo = generateUserInfo(response)
        if (userInfo == CognitoUserInfo("", "", "")) {
            return@get call.respondText(
                "Internal Server Error",
                status = HttpStatusCode.InternalServerError
            ) // Issue with the generation of the user info JSON.
        }

        call.respondText(
            Json.encodeToString<CognitoUserInfo>(userInfo),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }
}

/**
 * Sets up the callback route for the authentication process.
 */
fun Route.setUpCallbackRoute(route: String, redirectDomain: String = "http://localhost:5173") {
    get(route) {
        val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
        if (principal == null) {
            call.respond(HttpStatusCode.Unauthorized) // Authentication failed, do not grant access.
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
            return@get call.respond(HttpStatusCode.Unauthorized) // JWT token is invalid
        }
    }
}
