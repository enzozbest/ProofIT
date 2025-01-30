package kcl.seg.rtt.chat_history

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/chat") {
            call.respondText("Hello, world!")
        }

        post("/json") {
            try {
                val request = call.receive<Request>()
                println("Received request: $request")
                val response = Response(
                    time = LocalDateTime.now().toString(),
                    message = "${request.prompt}, ${request.userID}!"
                )
                call.respond(response)
            } catch (e: Exception) {
                println("Error processing request: ${e.message}")
                call.respondText(
                    text = "Invalid request: ${e.message}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        // terminal line to test this
        // Invoke-RestMethod -Uri "http://127.0.0.1:8000/json" -Method POST -Headers @{ "Content-Type" = "application/json" } -Body '{"userID":"user123","time":"2025-01-01T12:00:00","prompt":"Hello"}'

    }
}