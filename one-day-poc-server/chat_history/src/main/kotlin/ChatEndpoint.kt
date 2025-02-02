package kcl.seg.rtt.chat_history

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kcl.seg.rtt.chat_history.routes.*

fun Application.chatModule() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        chatRoutes()
        jsonRoutes()
        uploadRoutes()
    }
}