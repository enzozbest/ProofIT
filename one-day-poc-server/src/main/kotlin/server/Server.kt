package server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.coroutines.runBlocking

val frontendHost = Pair("localhost:5173", listOf("http"))

fun Application.setUp() {
    configurePlugins()
    seedTemplateLibrary()
}

fun Application.configurePlugins() {
    configureCORS(
        listOf(frontendHost),
        listOf(HttpMethod.Get, HttpMethod.Options),
        listOf(HttpHeaders.Authorization, HttpHeaders.ContentType),
        credentials = true,
    )
    configureContentNegotiation()
}

fun Application.seedTemplateLibrary() =
    runBlocking {
        TemplateLibrarySeeder.seed()
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
