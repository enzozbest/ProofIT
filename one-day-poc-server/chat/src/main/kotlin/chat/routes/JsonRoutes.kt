package chat.routes

import chat.JSON
import chat.Request
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import prompting.PromptingMain
import prompting.ServerResponse
import chat.storage.*
import java.util.UUID
import database.tables.chats.Conversation
import database.tables.chats.ChatMessage

private var promptingMainInstance: PromptingMain = PromptingMain()

fun Route.jsonRoutes() {
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
 * Processes a validated JSON request by passing it to the prompting pipeline.
 *
 * This function takes the prompt from the received request, sends it through
 * the prompting workflow, and returns the generated response to the client.
 *
 * @param request The validated Request object containing the user's prompt
 * @param call The ApplicationCall used to send the response back to the client
 * @throws NullPointerException if the prompting workflow returns a null response
 */
private suspend fun handleJsonRequest(
    request: Request,
    call: ApplicationCall,
) {
    val conversationId = request.conversationId
    val userMessage = ChatMessage(
        conversationId = request.conversationId,
        senderId = request.userID,
        content = request.prompt
    )
    storeMessage(userMessage)
    println("Stored user message: ${userMessage.id}")

    val response = getPromptingMain().run(request.prompt)
    println("RECEIVED RESPONSE")
    val jsonString = Json.encodeToString(ServerResponse.serializer(), response)
    println("ENCODED RESPONSE: $jsonString")

    val aiMessage = ChatMessage(
        conversationId = conversationId,
        senderId = "LLM",
        content = jsonString
    )
    storeMessage(aiMessage)
    println("Stored AI response: ${aiMessage.id}")

    call.respondText(jsonString, contentType = ContentType.Application.Json)
}

/**
 * This function serves as a getter for the singleton promptingMainInstance
 * to ensure consistent access throughout the application.
 *
 * @return The current PromptingMain instance
 */
private fun getPromptingMain(): PromptingMain = promptingMainInstance

/**
 * Sets a custom PromptingMain instance.
 *
 * This function is primarily used for testing purposes to inject a mock or
 * customized PromptingMain implementation.
 *
 * @param promptObject The PromptingMain instance to use for processing requests
 */
internal fun setPromptingMain(promptObject: PromptingMain) {
    promptingMainInstance = promptObject
}

/**
 * Resets the PromptingMain instance to a new default instance.
 *
 * This function is used to restore the default behavior of the prompting
 * workflow, typically after testing or when a fresh state is required.
 */
internal fun resetPromptingMain() {
    promptingMainInstance = PromptingMain()
}
