package kcl.seg.rtt.auth.authentication

import com.auth0.jwt.JWT
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.date.GMTDate
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.AUTHENTICATION_CHECK_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.AUTHENTICATION_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.CALL_BACK_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.JWT_VALIDATION_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.LOG_OUT_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.USER_INFO_ROUTE
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.Redis

object AuthenticationRoutes {
    const val AUTHENTICATION_ROUTE: String = "/api/auth"
    const val AUTHENTICATION_CHECK_ROUTE: String = "/api/auth/check"
    const val CALL_BACK_ROUTE: String = "/api/auth/callback"
    const val LOG_OUT_ROUTE: String = "/api/auth/logout"
    const val JWT_VALIDATION_ROUTE: String = "/api/auth/validate"
    const val USER_INFO_ROUTE: String = "api/auth/me"
}

/**
 * Configures the routes that will be used for authentication.
 */
internal fun Application.configureAuthenticationRoutes(authName: String) {
    routing {
        authenticate(authName) {
            setAuthenticationEndpoint(AUTHENTICATION_ROUTE)
            setUpCallbackRoute(CALL_BACK_ROUTE)
        }
        authenticate("jwt-verifier") {
            setUpJWTValidation(JWT_VALIDATION_ROUTE)
            setUpUserInfoRoute(USER_INFO_ROUTE)
        }
        setUpCheckEndpoint(AUTHENTICATION_CHECK_ROUTE)
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
        val cookie =
            call.request.cookies["AuthenticatedSession"]?.let {
                kotlin.runCatching { Json.decodeFromString<AuthenticatedSession>(it) }.getOrNull()
            } ?: return@post call.respond(HttpStatusCode.OK) // User already logged out, nothing to do.

        call.sessions.clear<AuthenticatedSession>()
        call.response.cookies.append(
            Cookie(
                "AuthenticatedSession",
                "",
                path = "/",
                httpOnly = true,
                secure = true,
                expires = GMTDate(0),
            ),
        )
        // Remove cached session
        Redis.getRedisConnection().use { jedis ->
            jedis.del("auth:${cookie.token}")
        }
        call.respond(HttpStatusCode.OK)
    }
}

/**
 * Sets up JWT validation route.
 * This route checks for credentials in two places: first the AuthenticatedSession cookie,
 * then the Authorization header. If the credentials are found and the token is valid, the route responds
 * with a JWTValidationResponse object. Otherwise, the route responds with an Unauthorized status code.
 * @param validationRoute The route (as a string) to set up the JWT validation on.
 */
internal fun Route.setUpJWTValidation(validationRoute: String) {
    get(validationRoute) {
        val token: String? =
            call.request.cookies["AuthenticatedSession"]?.let {
                runCatching { Json.decodeFromString<AuthenticatedSession>(it) }.getOrNull()?.token
            } ?: call.request.headers["Authorization"]?.removePrefix("Bearer ")

        val response = validateJWT(token)
        return@get respondAuthenticationCheckRequest(response, token)
    }
}

/**
 * Sets up a route to check the authentication status of a user.
 * If the user is authenticated, the route responds with the JWTValidationResponse object.
 * Otherwise, the route responds with an Unauthorized status code.
 * @param checkRoute The route (as a string) to set up the check on.
 */
internal fun Route.setUpCheckEndpoint(checkRoute: String) {
    get(checkRoute) {
        val sessionCookie =
            call.request.cookies["AuthenticatedSession"]?.let { cookie ->
                kotlin.runCatching { Json.decodeFromString<AuthenticatedSession>(cookie) }.getOrNull()
            } ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or missing session cookie")

        checkCache(sessionCookie.token)?.let { cachedSession ->
            return@get call.respond(HttpStatusCode.OK, cachedSession)
        }

        call.response.headers.append(HttpHeaders.Authorization, "Bearer ${sessionCookie.token}")
        call.response.headers.append(HttpHeaders.Location, "http://localhost:8000/api/auth/validate")
        call.respond(HttpStatusCode.TemporaryRedirect)
    }
}

/**
 * Sets up a route to retrieve user information from the relevant authentication provider.
 */
internal fun Route.setUpUserInfoRoute(
    route: String,
    verifierUrl: String = "https://cognito-idp.eu-west-2.amazonaws.com/",
    contentType: String = "application/x-amz-json-1.1",
    amzTarget: Boolean = true,
    amzApi: String = "AWSCognitoIdentityProviderService.GetUser",
) {
    get(route) {
        val sessionJson =
            call.request.cookies["AuthenticatedSession"] ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                "Missing authentication cookie",
            )

        val token =
            try {
                Json.decodeFromString<AuthenticatedSession>(sessionJson).token
            } catch (e: SerializationException) {
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
                status = HttpStatusCode.InternalServerError,
            ) // Issue with the generation of the user info JSON.
        }

        call.respondText(
            Json.encodeToString<CognitoUserInfo>(userInfo),
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json,
        )
    }
}

/**
 * Sets up the callback route for the authentication process.
 */
internal fun Route.setUpCallbackRoute(
    route: String,
    redirectDomain: String = "http://localhost:5173",
) {
    get(route) {
        val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
        val token: String =
            principal?.let {
                it.extraParameters["id_token"]
            } ?: return@get call.respond(HttpStatusCode.Unauthorized) // Authentication failed, do not grant access.

        val decoded =
            runCatching { JWT.decode(token) }.getOrElse {
                return@get call.respond(HttpStatusCode.Unauthorized)
            }

        val userId: String =
            decoded.getClaim("sub").asString() ?: return@get call.respond(HttpStatusCode.Unauthorized)
        val admin: Boolean =
            decoded.getClaim("cognito:groups").asList(String::class.java)?.contains("admin_users") == true

        call.sessions.set(AuthenticatedSession(userId, principal.accessToken, admin))
        cacheSession(token, JWTValidationResponse(userId, admin))
        val redirectUrl = call.request.queryParameters["redirect"] ?: "/"
        call.respondRedirect("$redirectDomain$redirectUrl")
    }
}
