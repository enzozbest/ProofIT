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

// dummy function
fun RetrievePrototypeResponse(output: String): Any {
    return output // Or return a proper response object
}

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

    // Working with ID to retrieve a prototype
    // rather than callLLM in PrototypeService
    // which uses strings to generate a prototype
    get("/prototype/{id}") {
        val prototypeId = call.parameters["id"]
        if (prototypeId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Prototype ID is missing.")
            return@get
        }

        // placeholder method retrievePrototype
        // To be replaced with our own logic

        //val prototypeString: String? = prototypeService.retrievePrototype(prototypeId)
        val prototypeString = "prototypestring"

        // Assumes prototypeService will return also some ID associated

        /**
        if (prototypeString == null) {
            call.respond(HttpStatusCode.NotFound, "No prototype found for ID: $prototypeId")
        } else {
            call.respond(HttpStatusCode.OK, RetrievePrototypeResponse(prototypeString))
        }
        */
        call.respond(HttpStatusCode.OK, RetrievePrototypeResponse(prototypeString))
    }
}


