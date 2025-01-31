package kcl.seg.rtt.auth

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.date.*
import kcl.seg.rtt.utils.JSON.PoCJSON.findCognitoUserAttribute
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

const val AUTHENTICATION_ROUTE: String = "/api/auth"
const val CALL_BACK_ROUTE: String = "/api/auth/callback"
const val LOG_OUT_ROUTE: String = "/api/logout"
const val JWT_VALIDATION_ROUTE: String = "/api/auth/me"
const val USER_INFO_ROUTE: String = "api/auth/userinfo"

/**
 * Configures the routes that will be used for authentication.
 */
fun Application.configureAuthenticationRoutes(authName: String = "Cognito") {
    setupSessions()
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
private fun Route.setUpJWTValidation(validationRoute: String) {
    get(validationRoute) {
        val sessionCookie =
            Json.decodeFromString<AuthenticatedSession>(call.request.cookies["AuthenticatedSession"] ?: "")
        call.respond(mapOf("userId" to sessionCookie.userId, "admin" to sessionCookie.admin))
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
            val userinfo = generateResponseJson(response)
            call.respond(HttpStatusCode.OK, userinfo)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

private fun generateResponseJson(response: Response): CognitoUserInfo {
    val jsonResponse = Json.parseToJsonElement(response.body?.string() ?: "{}").jsonObject
    val attributes = jsonResponse["UserAttributes"]?.jsonArray ?: return CognitoUserInfo("", "", "")

    return CognitoUserInfo(
        name = findCognitoUserAttribute(attributes, "name") ?: "Unknown",
        email = findCognitoUserAttribute(attributes, "email") ?: "Unknown",
        dob = findCognitoUserAttribute(attributes, "birthdate") ?: "Unknown"
    )
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
        val redirectUrl = call.request.queryParameters["redirect"] ?: "/"
        call.respondRedirect("localhost:5173$redirectUrl")
    }
}

/**
 * Sets up the sessions for the application.
 */
private fun Application.setupSessions() {
    install(Sessions) {
        cookie<AuthenticatedSession>("AuthenticatedSession") {
            cookie.maxAgeInSeconds = 3600
            cookie.secure = true
            cookie.httpOnly = true
        }
    }
}
