package kcl.seg.rtt.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello")
        }
        post("/api/chat/send") {
            var time = System.currentTimeMillis()
            call.respondText("This is the LLM response $time", status = HttpStatusCode.OK)
        }
    }
}
