package prompting.helpers

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import prompting.helpers.promptEngineering.Response
import java.time.LocalDateTime

/**
 * A utility singleton for handling HTTP responses from prompting services.
 *
 * This object provides methods to process HTTP responses from LLM or prompting
 * services and convert them into standardized application responses.
 */
object ResponseHandler {
    /**
     * Handles an HTTP response from a prompting service and sends an appropriate response to the client.
     *
     * @param response The HTTP response received from the prompting service
     * @param call The ApplicationCall context for sending the response to the client
     */
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

    /**
     * Processes a successful HTTP response from a prompting service.
     *
     * @param prototypeResponse The successful HTTP response from the prompting service
     * @param call The ApplicationCall context for sending the response to the client
     */
    private suspend fun handleSuccessResponse(
        prototypeResponse: HttpResponse,
        call: ApplicationCall,
    ) {
        val response = createResponse(prototypeResponse.bodyAsText())
        call.respond(response)
    }

    /**
     * Processes a failed HTTP response from a prompting service.

     * @param prototypeResponse The failed HTTP response from the prompting service
     * @param call The ApplicationCall context for sending the response to the client
     */
    private suspend fun handleFailureResponse(
        prototypeResponse: HttpResponse,
        call: ApplicationCall,
    ) {
        val response = createResponse("Error: ${prototypeResponse.status}, ${prototypeResponse.bodyAsText()}")
        call.respond(response)
    }

    /**
     * Creates a standardized Response object with the current timestamp and provided message.
     *
     * @param message The message to include in the response
     * @return A Response object with the current time and provided message
     */
    private fun createResponse(message: String): Response =
        Response(
            time = LocalDateTime.now().toString(),
            message = message,
        )
}
