package kcl.seg.rtt.prototype

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


fun Application.prototypeModule() {

    val ollamaService = OllamaService()
    val prototypeService = PrototypeService(ollamaService)

    prototypeRoutes(prototypeService)
}