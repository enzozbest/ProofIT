package kcl.seg.rtt.prototype

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable


object PrototypeRoutes {
    const val BASE = "/api/prototype"
    const val HEALTH: String = "$BASE/health"
    const val GENERATE: String = "$BASE/generate"
    const val RETRIEVE: String = "$BASE/retrieve"
}

// Request and Response DTOs
@Serializable
data class GenerateRequest(val prompt: String)


@Serializable
data class ErrorResponse(val error: String)


fun Application.prototypeRoutes(prototypeService: PrototypeService) {
    routing {
        healthCheck()
        generatePrototype(prototypeService)
        retrievePrototypeById(prototypeService)
    }

}

private fun Route.healthCheck() {
    get(PrototypeRoutes.HEALTH) {
        call.respond(HttpStatusCode.OK, "OK")
    }
}

private fun Route.generatePrototype(prototypeService: PrototypeService) {
    post(PrototypeRoutes.GENERATE) {
        try {
            val request = call.receive<GenerateRequest>()
            prototypeService.generatePrototype(request.prompt)
                .onSuccess { llmResponse ->
                    call.respond(HttpStatusCode.OK, llmResponse)
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Failed to generate prototype: ${error.message}")
                    )
                }
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid request format: ${e.message}")
            )
        }
    }
}

// Extension function for consistent error handling
private suspend fun ApplicationCall.respondError(error: Throwable) {
    respond(
        HttpStatusCode.BadRequest,
        ErrorResponse("Error: ${error.message ?: "Unknown error"}")
    )
}

/**
 * Utility function to get a valid prototypeId from URL parameters.
 * @return null if missing or blank.
 */
private fun getValidPrototypeId(call: ApplicationCall): String? {
    val id = call.parameters["id"]
    return if (id.isNullOrBlank()) null else id
}

/**
 * Retrieves a prototype by ID.
 * Responds with a redirect to the WebContainer if found.
 * @param prototypeService The [PrototypeService] to use for retrieving prototype content.
 * @see getValidPrototypeId
 * @see PrototypeService.retrievePrototype
 */
private fun Route.retrievePrototypeById(prototypeService: PrototypeService) {
    get(PrototypeRoutes.RETRIEVE) {
        val prototypeId = getValidPrototypeId(call)
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Prototype ID is missing.")

        val prototypeString: String? = prototypeService.retrievePrototype(prototypeId)

        if (prototypeString == null) {
            call.respond(HttpStatusCode.NotFound, "No prototype found for ID: $prototypeId")
        }
        else {
            call.respondText(prototypeString, ContentType.Text.Html)
        }
    }
}



