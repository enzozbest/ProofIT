package kcl.seg.rtt.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.CORS



fun Application.configureRouting() {
    install(CORS) {
        allowHost("localhost:5173")
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowCredentials = true
    }

    routing {
        get("/") {
            call.respondText("Hello")
        }
        post("/api/chat/send") {
            call.respondText("This is the LLM response", status= HttpStatusCode.OK)
        }
    }
}
