package kcl.seg.rtt.auth

import kotlinx.serialization.Serializable

@Serializable
sealed class UserSession {

    fun isAdmin(): Boolean {
        return when (this) {
            is GuestSession -> false
            is AuthenticatedSession -> admin
        }
    }
    
    fun getAccessToken(): String? {
        return when (this) {
            is GuestSession -> null
            is AuthenticatedSession -> token
        }
    }

    fun getSubmissions(): Int? {
        return when (this) {
            is GuestSession -> submissions
            is AuthenticatedSession -> null
        }
    }

    fun getSessionType(): String {
        return when (this) {
            is GuestSession -> "GuestSession"
            is AuthenticatedSession -> "AuthenticatedSession"
        }
    }
}

data class GuestSession(val guestId: String, val submissions: Int) : UserSession()
data class AuthenticatedSession(val userId: String, val token: String, val admin: Boolean) : UserSession()

