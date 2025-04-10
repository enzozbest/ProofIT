package server

import authentication.authentication.AuthenticatedSession
import authentication.authentication.configureAuthenticators
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import utils.json.PoCJSON

val frontendHost = Pair("localhost:5173", listOf("http"))

fun Application.configurePlugins() {
    configureCORS(
        listOf(frontendHost),
        listOf(HttpMethod.Get, HttpMethod.Options),
        listOf(HttpHeaders.Authorization, HttpHeaders.ContentType),
        credentials = true,
    )
    configureContentNegotiation()
    configureAuthentication("cognito.json")
}

internal fun Application.configureAuthentication(configFilePath: String) {
    val config = PoCJSON.readJsonFile(configFilePath)
    install(Authentication) {
        configureAuthenticators(config)
    }
    configureSessions()
}

/**
 * Sets up the sessions for the application.
 */
private fun Application.configureSessions() {
    install(Sessions) {
        cookie<AuthenticatedSession>("AuthenticatedSession") {
            cookie.maxAgeInSeconds = 3600L
            cookie.secure = true
            cookie.httpOnly = true
            cookie.path = "/"
            cookie.extensions["SameSite"] = "None"
        }
    }
}

private fun Application.configureCORS(
    hosts: List<Pair<String, List<String>>>,
    methods: List<HttpMethod>,
    headers: List<String>,
    credentials: Boolean = false,
) {
    install(CORS) {
        for (method in methods) {
            allowMethod(method)
        }
        for (header in headers) {
            allowHeader(header)
        }
        for (host in hosts) {
            allowHost(host.first, schemes = host.second)
        }

        // for iframe embedding
        allowNonSimpleContentTypes = true

        allowCredentials = credentials
    }
}

private fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        json()
    }
}
