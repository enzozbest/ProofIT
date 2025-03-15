package authentication

import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import redis.Redis
import utils.json.PoCJSON
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.util.Date
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
 * Builds an OkHttp Request for the Cognito GetUser API with AWS Signature Version 4.
 *
 * @param token The Cognito Access Token.
 * @param verifierUrl The endpoint URL (e.g., "https://cognito-idp.us-west-2.amazonaws.com/").
 * @param accessKey Your AWS access key.
 * @param secretKey Your AWS secret key.
 * @param region The AWS region, e.g. "us-west-2".
 * @param contentType The content type, defaults to "application/x-amz-json-1.1".
 * @param amzTarget Whether to include the X-Amz-Target header.
 * @param amzApi The X-Amz-Target header value (defaults to "AWSCognitoIdentityProviderService.GetUser").
 * @param amzDate The full timestamp (e.g., "20230613T200059Z").
 * @param dateStamp The date stamp (e.g., "20230613").
 * @return An OkHttp [Request] object ready to be executed.
 */
internal fun buildUserInfoRequest(
    token: String,
    verifierUrl: String,
    accessKey: String,
    secretKey: String,
    region: String,
    contentType: String = "application/x-amz-json-1.1",
    amzTarget: Boolean,
    amzApi: String = "AWSCognitoIdentityProviderService.GetUser",
    amzDate: String,
    dateStamp: String,
): Request {
    val payload = "{\"AccessToken\": \"$token\"}"
    val authorizationHeader =
        computeAWSSignature(
            method = "POST",
            service = "cognito-idp",
            region = region,
            endpoint = verifierUrl,
            amzTarget = if (amzTarget) amzApi else null,
            contentType = contentType,
            payload = payload,
            accessKey = accessKey,
            secretKey = secretKey,
            amzDate = amzDate,
            dateStamp = dateStamp,
        )

    val jsonMediaType = contentType.toMediaTypeOrNull()
    val requestBody = payload.toRequestBody(jsonMediaType)

    val requestBuilder =
        Request
            .Builder()
            .url(verifierUrl)
            .post(requestBody)
            .addHeader("Content-Type", contentType)
            .addHeader("X-Amz-Date", amzDate)
            .addHeader("Authorization", authorizationHeader)

    if (amzTarget) {
        requestBuilder.addHeader("X-Amz-Target", amzApi)
    }
    return requestBuilder.build()
}

/**
 * Extension function on [Request] objects to send the request and return the response.
 * @return The response of the request.
 */
internal fun Request.sendRequest(): Response = OkHttpClient().newCall(this).execute()

private fun hmacSHA256(
    key: ByteArray,
    data: String,
): ByteArray {
    val algorithm = "HmacSHA256"
    val mac = Mac.getInstance(algorithm)
    val keySpec = SecretKeySpec(key, algorithm)
    mac.init(keySpec)
    return mac.doFinal(data.toByteArray(Charsets.UTF_8))
}

private fun sha256Hex(data: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(data.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

private fun getSignatureKey(
    secretKey: String,
    dateStamp: String,
    regionName: String,
    serviceName: String,
): ByteArray {
    val kSecret = ("AWS4$secretKey").toByteArray(Charsets.UTF_8)
    val kDate = hmacSHA256(kSecret, dateStamp)
    val kRegion = hmacSHA256(kDate, regionName)
    val kService = hmacSHA256(kRegion, serviceName)
    return hmacSHA256(kService, "aws4_request")
}

/**
 * Computes the AWS Signature Version 4 Authorization header.
 *
 * @param method The HTTP method, e.g. "POST".
 * @param service The AWS service name, e.g. "cognito-idp".
 * @param region The AWS region, e.g. "us-west-2".
 * @param endpoint The full endpoint URL.
 * @param amzTarget If non-null, the value for the X-Amz-Target header.
 * @param contentType The content type (e.g., "application/x-amz-json-1.1").
 * @param payload The JSON request body.
 * @param accessKey Your AWS access key.
 * @param secretKey Your AWS secret key.
 * @param amzDate The timestamp in the format "yyyyMMdd'T'HHmmss'Z'", e.g. "20230613T200059Z".
 * @param dateStamp The date stamp in the format "yyyyMMdd", e.g. "20230613".
 * @return The complete Authorization header value.
 */
internal fun computeAWSSignature(
    method: String,
    service: String,
    region: String,
    endpoint: String,
    amzTarget: String?,
    contentType: String,
    payload: String,
    accessKey: String,
    secretKey: String,
    amzDate: String,
    dateStamp: String,
): String {
    val urlObj = URI(endpoint)
    val host = urlObj.host
    val canonicalUri = if (urlObj.path.isEmpty()) "/" else urlObj.path
    val canonicalQueryString = ""

    val headersMap = TreeMap<String, String>()
    headersMap["content-type"] = contentType
    headersMap["host"] = host
    headersMap["x-amz-date"] = amzDate
    if (amzTarget != null) {
        headersMap["x-amz-target"] = amzTarget
    }
    val canonicalHeaders = headersMap.entries.joinToString(separator = "\n") { "${it.key}:${it.value.trim()}" } + "\n"
    val signedHeaders = headersMap.keys.joinToString(separator = ";")

    val payloadHash = sha256Hex(payload)

    val canonicalRequest =
        "$method\n$canonicalUri\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

    val algorithm = "AWS4-HMAC-SHA256"
    val credentialScope = "$dateStamp/$region/$service/aws4_request"
    val canonicalRequestHash = sha256Hex(canonicalRequest)
    val stringToSign = "$algorithm\n$amzDate\n$credentialScope\n$canonicalRequestHash"

    val signingKey = getSignatureKey(secretKey, dateStamp, region, service)
    val signatureBytes = hmacSHA256(signingKey, stringToSign)
    val signature = signatureBytes.joinToString("") { "%02x".format(it) }

    return "$algorithm Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
}

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
