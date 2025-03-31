package chat.routes

import chat.JSON
import chat.Request
import chat.storage.getPreviousPrototype
import chat.storage.updateConversationName
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import prompting.PredefinedPrototypes
import prompting.PromptingMain
import java.time.Instant

/**
 * Sets up the main JSON route for handling chat requests.
 *
 * This route receives JSON requests containing prompts and processes them
 * through the prompting pipeline.
 */
internal fun Route.setJsonRoute() {
    post(JSON) {
        val request: Request =
            runCatching {
                call.receive<Request>()
            }.getOrElse {
                return@post call.respondText(
                    "Invalid request ${it.message}",
                    status = HttpStatusCode.BadRequest,
                )
            }
        handleJsonRequest(request, call)
    }
}

/**
 * Sets up the route for conversation renaming.
 *
 * This route allows clients to rename existing conversations.
 */
internal fun Route.setJsonRouteRetrieval() {
    post("$JSON/{conversationId}/rename") {
        try {
            val conversationId = call.parameters["conversationId"] ?: throw IllegalArgumentException("Missing ID")
            val requestBody = call.receive<Map<String, String>>()
            val name = requestBody["name"] ?: throw IllegalArgumentException("Missing name")
            val success = updateConversationName(conversationId, name)
            if (success) {
                call.respondText("Conversation renamed successfully", status = HttpStatusCode.OK)
            } else {
                call.respondText("Failed to update name", status = HttpStatusCode.InternalServerError)
            }
        } catch (e: Exception) {
            call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.BadRequest,
            )
        }
    }
}

/**
 * Processes a validated JSON request by passing it to the prompting pipeline.
 *
 * This function takes the prompt from the received request, sends it through
 * the prompting workflow, and returns the generated response to the client.
 * It avoids unnecessary deserialization/serialization while maintaining the expected response format.
 *
 * @param request The validated Request object containing the user's prompt
 * @param call The ApplicationCall used to send the response back to the client
 */
private suspend fun handleJsonRequest(
    request: Request,
    call: ApplicationCall,
) {
    if (request.prompt.isBlank()) {
        return call.respondText(
            "Invalid request: Empty prompt",
            status = HttpStatusCode.BadRequest,
        )
    }

    try {
        val chatContent: String
        val prototypeFilesJson: String?

        if (request.predefined) {
            val predefinedResponse = PredefinedPrototypes.run(request.prompt)

            chatContent = predefinedResponse.chatMessage
            prototypeFilesJson = predefinedResponse.files
        } else {
            val previousGenerationJson = getPreviousPrototype(request.conversationId)?.filesJson
            MessageHandler.saveMessage(request.conversationId, request.userID, request.prompt)

            val promptJsonResponse = PromptingMainProvider.getInstance().run(request.prompt, previousGenerationJson)

            val processed = JsonProcessor.processRawJsonResponse(promptJsonResponse)
            chatContent = processed.first
            prototypeFilesJson = processed.second
        }

        val messageId = MessageHandler.savePrototype(request.conversationId, chatContent, prototypeFilesJson)

        val finalResponse =
            """
            {
                "chat": {
                    "message": "${chatContent.trim()}",
                    "role": "LLM",
                    "timestamp": "${Instant.now()}",
                    "messageId": "$messageId",
                    "conversationId": "${request.conversationId}"
                },
                "prototype": {
                    "files": ${prototypeFilesJson.trim()}
                }
            }
            """.trimIndent()

        call.respondText(finalResponse, contentType = ContentType.Application.Json)
    } catch (e: Exception) {
        call.respondText(
            "Error processing request: ${e.message}",
            status = HttpStatusCode.InternalServerError,
        )
    }
}

/**
 * Sets a custom PromptingMain instance.
 *
 * This function is primarily used for testing purposes to inject a mock or
 * customized PromptingMain implementation.
 *
 * @param promptObject The PromptingMain instance to use for processing requests
 */
internal fun setPromptingMain(promptObject: PromptingMain) {
    PromptingMainProvider.setInstance(promptObject)
}

/**
 * Resets the PromptingMain instance to a new default instance.
 *
 * This function is used to restore the default behavior of the prompting
 * workflow, typically after testing or when a fresh state is required.
 */
internal fun resetPromptingMain() {
    PromptingMainProvider.resetInstance()
}
