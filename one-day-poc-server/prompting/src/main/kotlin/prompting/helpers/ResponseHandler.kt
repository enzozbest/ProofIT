package prompting.helpers

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import prompting.helpers.promptEngineering.Response
import java.time.LocalDateTime

object ResponseHandler {
    suspend fun handlePromptResponse(
        response: HttpResponse,
        call: ApplicationCall,
    ) {
        if (response.status.isSuccess()) {
            handleSuccessResponse(response, call)
        } else {
            handleFailureResponse(response, call)
        }
    }

    private suspend fun handleSuccessResponse(
        prototypeResponse: HttpResponse,
        call: ApplicationCall,
    ) {
        val response = createResponse(prototypeResponse.bodyAsText())
        call.respond(response)
    }

    private suspend fun handleFailureResponse(
        prototypeResponse: HttpResponse,
        call: ApplicationCall,
    ) {
        println("Error: ${prototypeResponse.status}")
        val response = createResponse("Error: ${prototypeResponse.status}, ${prototypeResponse.bodyAsText()}")
        call.respond(response)
    }

    private fun createResponse(message: String): Response =
        Response(
            time = LocalDateTime.now().toString(),
            message = message,
        )

//    private suspend fun handleError(
//        e: Exception,
//        call: ApplicationCall,
//    ) {
//        call.respondText(
//            text = "Invalid request: ${e.message}",
//            status = HttpStatusCode.BadRequest,
//        )
//    }
}
