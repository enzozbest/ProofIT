package kcl.seg.rtt.auth.authentication

import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kcl.seg.rtt.utils.json.PoCJSON
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import redis.Redis
import java.time.Instant
import java.util.Date

/**
 * Data class representing the information of a user's authenticated session in the API.
 */
@Serializable
internal data class AuthenticatedSession(
    val userId: String,
    val token: String,
    val admin: Boolean?,
)

/**
 * Data class representing user's information which is relevant for the client application.
 */
@Serializable
internal data class CognitoUserInfo(
    val name: String,
    val email: String,
    val dob: String,
)

/**
 * Data class representing the response of a validation request to the JWT verifier.
 */
@Serializable
internal data class JWTValidationResponse(
    val userId: String,
    val admin: Boolean?,
)

/**
 * Function to generate a [CognitoUserInfo] object from a response.
 * @param response The response from the Cognito API.
 * @return A [CognitoUserInfo] object.
 */
internal fun generateUserInfo(response: Response): CognitoUserInfo {
    val jsonResponse = Json.parseToJsonElement(response.body.string()).jsonObject
    val attributes = jsonResponse["UserAttributes"]?.jsonArray ?: return CognitoUserInfo("", "", "")

    return CognitoUserInfo(
        name = PoCJSON.findCognitoUserAttribute(attributes, "name") ?: "Unknown",
        email = PoCJSON.findCognitoUserAttribute(attributes, "email") ?: "Unknown",
        dob = PoCJSON.findCognitoUserAttribute(attributes, "birthdate") ?: "Unknown",
    )
}

/**
 * Function to generate a [Request] object from a set of parameters.
 * @param token The token to be used in the request.
 * @param verifierUrl The URL of the verifier.
 * @param contentType The content type of the request.
 * @param amzTarget Whether the request is an Amazon API.
 * @param amzApi The Amazon API to be used.
 * @return A [Request] object.
 */
internal fun buildUserInfoRequest(
    token: String = "",
    verifierUrl: String,
    contentType: String,
    amzTarget: Boolean,
    amzApi: String,
): Request {
    val request =
        Request
            .Builder()
            .url(verifierUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", contentType)

    if (amzTarget) {
        request.addHeader("X-Amz-Target", amzApi)
    }

    return request.build()
}

/**
 * Extension function on [Request] objects to send the request and return the response.
 * @return The response of the request.
 */
internal fun Request.sendRequest(): Response = OkHttpClient().newCall(this).execute()

/**
 * Function to cache a session in Redis.
 * @param token The token to be cached.
 * @param authData The authentication data to be cached.
 * @param expirySeconds The expiry time of the cache in seconds.
 */
internal fun cacheSession(
    token: String,
    authData: JWTValidationResponse,
    expirySeconds: Long = 3600,
) {
    Redis.getRedisConnection().use { jedis ->
        jedis.setex("auth:$token", expirySeconds, Json.encodeToString(authData))
    }
}

/**
 * Function to check the cache for an authentication session.
 * @param token The token to be checked.
 * @return The authentication data if it exists in the cache, or null if it does not.
 */
internal fun checkCache(token: String): JWTValidationResponse? {
    Redis.getRedisConnection().use { jedis ->
        val cachedData = jedis["auth:$token"] ?: return null
        return Json.decodeFromString<JWTValidationResponse>(cachedData)
    }
}

/**
 * Function to validate a JWT token.
 * The function decodes the token and checks if it is expired.
 * If the token is invalid, expired, or null, the function returns null.
 * If the token is valid, the function returns a [JWTValidationResponse] object.
 * @param token The token to be validated.
 * @return A [JWTValidationResponse] object if the token is valid, or null if it is not.
 */
internal fun validateJWT(token: String?): JWTValidationResponse? =
    runCatching { JWT.decode(token) }
        .getOrNull()
        ?.takeIf { it.expiresAt.after(Date.from(Instant.now())) }
        ?.let { decoded ->
            val userId = decoded.getClaim("sub").asString()
            val admin = decoded.getClaim("cognito:groups").asList(String::class.java)?.contains("admin_users") == true
            userId?.let { JWTValidationResponse(it, admin) }
        }

/**
 * Function to respond to an authentication request made through the /api/auth/check.
 * @param response The response to be sent. If null, the response will be an Unauthorized status.
 */
internal suspend fun RoutingContext.respondAuthenticationCheckRequest(
    response: JWTValidationResponse?,
    token: String?,
) {
    response?.let {
        cacheSession(token!!, response)
        call.respond(HttpStatusCode.OK, response)
        return
    }
    call.respond(HttpStatusCode.Unauthorized, "Invalid or missing credentials!")
}
