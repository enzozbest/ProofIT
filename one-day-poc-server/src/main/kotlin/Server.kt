package kcl.seg.rtt

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*

val frontendHost = Pair("localhost:5173", listOf("http"))

fun Application.configurePlugins() {
    configureCORS(
        listOf(frontendHost),
        listOf(HttpMethod.Get, HttpMethod.Options),
        listOf(HttpHeaders.Authorization, HttpHeaders.ContentType),
        credentials = true
    )
    configureContentNegotiation()
}

private fun Application.configureCORS(
    hosts: List<Pair<String, List<String>>>,
    methods: List<HttpMethod>,
    headers: List<String>,
    credentials: Boolean = false
) {
    install(CORS) {
        for (method in methods)
            allowMethod(method)
        for (header in headers)
            allowHeader(header)
        for (host in hosts)
            allowHost(host.first, schemes = host.second)

        allowCredentials = credentials
    }
}

private fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        json()
    }
}
