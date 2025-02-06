package kcl.seg.rtt.chat_history

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kcl.seg.rtt.chat_history.routes.*
import io.ktor.server.auth.*

fun Application.chatModule() {
    routing {
        authenticate("jwt-verifier") {
            chatRoutes()
            jsonRoutes()
            uploadRoutes()
        }
    }
}