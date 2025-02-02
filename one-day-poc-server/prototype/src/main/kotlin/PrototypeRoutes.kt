package kcl.seg.rtt.prototype

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import io.ktor.server.response.respond
import kcl.seg.rtt.prototype.LlmResponse

// Request and Response DTOs
@Serializable
data class GenerateRequest(val prompt: String)

@Serializable
data class GenerateResponse(val output: String)

@Serializable
data class ErrorResponse(val error: String)

fun Route.prototypeRoutes(prototypeService: PrototypeService) {
    route("/prototype") {
        get("/health") {
            call.respond(HttpStatusCode.OK, "OK")
        }

        post("/generate") {
            try {
                val request = call.receive<GenerateRequest>()
                val result = prototypeService.generatePrototype(request.prompt)

                result.onSuccess { llmResponse ->
                    call.respond(HttpStatusCode.OK, llmResponse)
                }.onFailure { error ->
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Error: ${error.message ?: "Unknown error"}"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Error: ${e.message ?: "Unknown error"}"))
            }
        }
    }
}