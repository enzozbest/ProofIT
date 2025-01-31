package kcl.seg.rtt.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticatedSession(val userId: String, val token: String, val admin: Boolean?)

@Serializable
data class CognitoUserInfo(val name: String, val email: String, val dob: String)