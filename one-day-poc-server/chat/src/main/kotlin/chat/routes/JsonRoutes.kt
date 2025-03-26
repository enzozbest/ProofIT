package chat.routes

import chat.JSON
import chat.Request
import chat.storage.getPreviousPrototype
import chat.storage.storeMessage
import chat.storage.storePrototype
import chat.storage.updateConversationName
import database.tables.chats.ChatMessage
import database.tables.chats.Prototype
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import prompting.PromptingMain
import java.time.Instant

/**
 * Represents a response from the chat processing system.
 *
 * @property message The generated text response from the LLM
 * @property role The role of the responder (default: "LLM")
 * @property timestamp Timestamp string indicating when the response was created
 */
@Serializable
data class ChatResponse(
    val message: String,
    val role: String = "LLM",
    val timestamp: String,
    val messageId: String,
)

@Serializable
data class ServerResponse(
    val chat: ChatResponse,
    val prototype: PrototypeResponse? = null,
)

@Serializable
data class PrototypeResponse(
    val files: JsonObject, // Keep as JsonObject, not Map
)

private lateinit var promptingMainInstance: PromptingMain

internal fun Route.setJsonRoute() {
    post(JSON) {
        println("Received JSON request")
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

internal fun Route.setJsonRouteRetrieval() {
    post("$JSON/{conversationId}/rename") {
        try {
            println("Received conversation rename request")
            val conversationId = call.parameters["conversationId"] ?: throw IllegalArgumentException("Missing ID")
            val requestBody = call.receive<Map<String, String>>()
            val name = requestBody["name"] ?: throw IllegalArgumentException("Missing name")
            val success = updateConversationName(conversationId, name)
            println("Renamed conversation $conversationId to $name")
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
 *
 * @param request The validated Request object containing the user's prompt
 * @param call The ApplicationCall used to send the response back to the client
 * @throws NullPointerException if the prompting workflow returns a null response
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

    println("Handling JSON request: ${request.prompt} from ${request.userID} for conversation ${request.conversationId}")
    saveMessage(request.conversationId, request.userID, request.prompt)
    val previousGeneration = getPreviousPrototype(request.conversationId)?.filesJson
    try {
        val promptResponse = getPromptingMain().run(request.prompt, previousGeneration)
        val jsonResponse = runBlocking { Json.decodeFromString<JsonObject>(promptResponse) }
        val response = serverResponse(jsonResponse)
        val id = savePrototype(request.conversationId, response.prototype!!.files)
        val responseWithId = response.copy(chat = response.chat.copy(messageId = id))
        val jsonString = Json.encodeToString(ServerResponse.serializer(), responseWithId)
        println("ENCODED RESPONSE: $jsonString")

        call.respondText(jsonString, contentType = ContentType.Application.Json)
    } catch (e: Exception) {
        println("Error in handleJsonRequest: ${e.message}")
        call.respondText(
            "Error processing request: ${e.message}",
            status = HttpStatusCode.InternalServerError,
        )
    }
}

private suspend fun saveMessage(
    conversationId: String,
    senderId: String,
    content: String,
): ChatMessage {
    val message =
        ChatMessage(
            conversationId = conversationId,
            senderId = senderId,
            content = content,
        )
    storeMessage(message)
    return message
}

private suspend fun savePrototype(
    conversationId: String,
    response: JsonObject,
): String {
    val savedMessage = saveMessage(conversationId, "LLM", response["chat"]!!.jsonPrimitive.content)
    response["prototype"]!!.jsonObject["files"]!!.let { prototypeResponse ->
        val prototype =
            Prototype(
                messageId = savedMessage.id,
                filesJson = prototypeResponse.jsonObject.toString(),
                version = 1,
                isSelected = true,
            )
        storePrototype(prototype)
    }
    return savedMessage.id
}

/**
 * Extracts the functional requirements and prototype files from the LLM response.
 * @param response The LLM response.
 * @return A [ServerResponse] containing both chat response and prototype files.
 */
private fun serverResponse(response: JsonObject): ServerResponse {
    val defaultResponse = "Here is your code."
    val chat =
        when (val jsonReqs = response["chat"]) {
            is JsonPrimitive -> jsonReqs.content
            is JsonObject -> jsonReqs["message"]?.jsonPrimitive?.content ?: defaultResponse
            else -> defaultResponse
        }
    val chatResponse =
        ChatResponse(
            message = chat,
            role = "LLM",
            timestamp = Instant.now().toString(),
            messageId = "0",
        )

    val prototypeResponse =
        response["prototype"]?.let { prototype ->
            if (prototype is JsonObject && prototype.containsKey("files")) {
                PrototypeResponse(
                    files = prototype["files"] as JsonObject,
                )
            } else {
                null
            }
        }
    return ServerResponse(
        chat = chatResponse,
        prototype = prototypeResponse,
    )
}

/**
 * This function serves as a getter for the singleton promptingMainInstance
 * to ensure consistent access throughout the application.
 *
 * @return The current PromptingMain instance
 */
private fun getPromptingMain(): PromptingMain {
    if (!::promptingMainInstance.isInitialized) {
        promptingMainInstance = PromptingMain()
    }
    return promptingMainInstance
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
