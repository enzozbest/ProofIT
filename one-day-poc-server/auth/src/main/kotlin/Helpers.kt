package kcl.seg.rtt.auth

import kcl.seg.rtt.utils.JSON.PoCJSON
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Data class representing the information of a user's authenticated session in the API.
 */
@Serializable
data class AuthenticatedSession(val userId: String, val token: String, val admin: Boolean?)

/**
 * Data class representing user's information which is relevant for the client application.
 */
@Serializable
data class CognitoUserInfo(val name: String, val email: String, val dob: String)

/**
 * Data class representing the response of a validation request to the JWT verifier.
 */
@Serializable
data class JWTValidationResponse(val userId: String, val admin: Boolean?)

/**
 * Function to generate a [CognitoUserInfo] object from a response.
 * @param response The response from the Cognito API.
 * @return A [CognitoUserInfo] object.
 */
fun generateUserInfo(response: Response): CognitoUserInfo {
    val jsonResponse = Json.parseToJsonElement(response.body?.string() ?: "{}").jsonObject
    val attributes = jsonResponse["UserAttributes"]?.jsonArray ?: return CognitoUserInfo("", "", "")

    return CognitoUserInfo(
        name = PoCJSON.findCognitoUserAttribute(attributes, "name") ?: "Unknown",
        email = PoCJSON.findCognitoUserAttribute(attributes, "email") ?: "Unknown",
        dob = PoCJSON.findCognitoUserAttribute(attributes, "birthdate") ?: "Unknown"
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
fun buildUserInfoRequest(
    token: String = "",
    verifierUrl: String,
    contentType: String,
    amzTarget: Boolean,
    amzApi: String
): Request {
    val request = Request.Builder()
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
fun Request.sendRequest(): Response {
    return OkHttpClient().newCall(this).execute()
}
