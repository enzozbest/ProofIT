package kcl.seg.rtt.chat_history

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.module() {
    routing {
        get("/chat") {
            call.respondText("Hello, world!")
        }
    }
}