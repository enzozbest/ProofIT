package kcl.seg.rtt.auth

import kcl.seg.rtt.utils.JSON.PoCJSON
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.Response

@Serializable
data class AuthenticatedSession(val userId: String, val token: String, val admin: Boolean?)

@Serializable
data class CognitoUserInfo(val name: String, val email: String, val dob: String)

@Serializable
data class JWTValidationResponse(val userId: String, val admin: Boolean?)

fun generateUserInfo(response: Response): CognitoUserInfo {
    val jsonResponse = Json.parseToJsonElement(response.body?.string() ?: "{}").jsonObject
    val attributes = jsonResponse["UserAttributes"]?.jsonArray ?: return CognitoUserInfo("", "", "")

    return CognitoUserInfo(
        name = PoCJSON.findCognitoUserAttribute(attributes, "name") ?: "Unknown",
        email = PoCJSON.findCognitoUserAttribute(attributes, "email") ?: "Unknown",
        dob = PoCJSON.findCognitoUserAttribute(attributes, "birthdate") ?: "Unknown"
    )
}

