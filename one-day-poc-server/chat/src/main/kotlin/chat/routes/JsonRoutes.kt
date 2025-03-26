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
import prompting.PromptingMain
import java.time.Instant
import kotlin.text.indexOf

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

    println("Handling JSON request: ${request.prompt} from ${request.userID} for conversation ${request.conversationId}")

    val previousGenerationJson = getPreviousPrototype(request.conversationId)?.filesJson

    try {
        // Get raw response from LLM
        val promptJsonResponse = getPromptingMain().run(request.prompt, previousGenerationJson)

        // Extract chat content and prototype files JSON
        val (chatContent, prototypeFilesJson) = processRawJsonResponse(promptJsonResponse)

        // Save to database and get message ID
        val messageId = savePrototype(request.conversationId, chatContent, prototypeFilesJson)

        // Construct a proper JSON response directly - no nested serialization
        val finalResponse =
            """
            {
                "chat": {
                    "message": "$chatContent",
                    "role": "LLM",
                    "timestamp": "${Instant.now()}",
                    "messageId": "$messageId"
                },
                "prototype": {
                    "files": $prototypeFilesJson
                }
            }
            """.trimIndent()

        call.respondText(finalResponse, contentType = ContentType.Application.Json)
    } catch (e: Exception) {
        println("Error in handleJsonRequest: ${e.message}")
        e.printStackTrace()
        call.respondText(
            "Error processing request: ${e.message}",
            status = HttpStatusCode.InternalServerError,
        )
    }
}

/**
 * Extract parts from the raw JSON response using string operations to avoid full deserialization.
 *
 * @param jsonString The raw JSON response from the prompting pipeline
 * @return Triple of (chatContent, prototypeContent, messageId) - any might be null if not found
 */
private fun processRawJsonResponse(jsonString: String): Pair<String, String> {
    // Simple regex to extract chat content
    val chatRegex = """"chat"\s*:\s*"([^"]*)"|\{\s*"message"\s*:\s*"([^"]*)"""".toRegex()
    val chatMatch = chatRegex.find(jsonString)
    val chatContent = chatMatch?.groupValues?.firstOrNull { it.isNotEmpty() && it != chatMatch.value } ?: ""

    // Extract prototype content using string indexing instead of regex
    var prototypeContent: String = "{}"

    // Find the "files" section within prototype
    val filesMarker = "\"files\":"
    val filesStartIndex = jsonString.indexOf(filesMarker)

    if (filesStartIndex >= 0) {
        // Start after the "files": marker
        var pos = filesStartIndex + filesMarker.length
        var depth = 0
        var foundStart = false
        val filesJson = StringBuilder()

        // Skip whitespace to find the opening brace
        while (pos < jsonString.length && !foundStart) {
            if (jsonString[pos] == '{') {
                foundStart = true
                depth = 1
                filesJson.append('{')
            }
            pos++
        }

        // If we found the opening brace, find the matching closing brace
        if (foundStart) {
            while (pos < jsonString.length && depth > 0) {
                val char = jsonString[pos]
                filesJson.append(char)

                if (char == '{') {
                    depth++
                } else if (char == '}') {
                    depth--
                }
                pos++
            }

            // If we found a complete JSON object, use it
            if (depth == 0) {
                prototypeContent = filesJson.toString()
            }
        }
    }

    // Extract messageId if present
    val messageIdRegex = """"messageId"\s*:\s*"([^"]*)"?""".toRegex()
    val messageIdMatch = messageIdRegex.find(jsonString)
    messageIdMatch?.groupValues?.get(1)

    return Pair(chatContent, prototypeContent)
}

/**
 * Saves a message to the database.
 *
 * @param conversationId The ID of the conversation
 * @param senderId The ID of the sender
 * @param content The content of the message
 * @return The saved ChatMessage
 */
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

/**
 * Stores a prototype and LLM message from string content.
 * This is an adapted version that works with string extraction rather than JsonObject deserialization.
 *
 * @param conversationId The ID of the conversation
 * @param chatContent The content for the LLM message
 * @param prototypeFilesJson The JSON string for the prototype files or null if no prototype
 * @return The ID of the saved message
 */
private suspend fun savePrototype(
    conversationId: String,
    chatContent: String,
    prototypeFilesJson: String?,
): String {
    val savedMessage = saveMessage(conversationId, "LLM", chatContent)

    if (prototypeFilesJson != null) {
        val prototype =
            Prototype(
                messageId = savedMessage.id,
                filesJson = prototypeFilesJson,
                version = 1,
                isSelected = true,
            )
        storePrototype(prototype)
    }

    return savedMessage.id
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
