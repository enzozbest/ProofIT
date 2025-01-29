package kcl.seg.rtt.prototype

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import io.ktor.server.response.respond

// Request and Response DTOs
@Serializable
data class GenerateRequest(val prompt: String)

@Serializable
data class GenerateResponse(val output: String)

@Serializable
data class ErrorResponse(val error: String)

fun Route.prototypeRoutes(prototypeService: PrototypeService) {
    post("/generate") {
        try {
            val request = call.receive<GenerateRequest>()
            val result = prototypeService.generatePrototype(request.prompt)
            val serializedResponse = GenerateResponse(result)
            call.respond(HttpStatusCode.OK, serializedResponse)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Error: ${e.message ?: "Unknown error"}"))
        }
    }
}