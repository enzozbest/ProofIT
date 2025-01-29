package kcl.seg.rtt.prototype

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// Request and Response DTOs
@Serializable
data class GenerateRequest(val prompt: String, val context: List<String>?)

@Serializable
data class GenerateResponse(val output: String)

fun Route.prototypeRoutes(prototypeService: PrototypeService) {
    post("/generate") {
        try {
            val request = call.receive<GenerateRequest>()
            val result = prototypeService.generatePrototype(request.prompt, request.context)
            call.respond(HttpStatusCode.OK, GenerateResponse(result))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Error: ${e.message ?: "Unknown error"}")
        }
    }
}