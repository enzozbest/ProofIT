package kcl.seg.rtt.prototype

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.prototypeModule() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    val ollamaService = OllamaService()
    val prototypeService = PrototypeService(ollamaService)

    prototypeRoutes(prototypeService)
}