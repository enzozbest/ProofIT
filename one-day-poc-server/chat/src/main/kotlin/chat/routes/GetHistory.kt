package chat.routes

import chat.GET
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.ZoneOffset
import chat.storage.*

@Serializable
data class Conversation(
    val id: String,
    val name: String,
    val lastModified: String,
    val messageCount: Int
)

@Serializable
data class ConversationHistory(
    val conversations: List<Conversation>
)

internal fun Route.chatRoutes() {
    get(GET) {
        try {
            val requestBody = call.receive<Map<String, String>>()
            val userId = requestBody["userId"] ?: throw IllegalArgumentException("Missing name")

            val conversations = getConversationHistory(userId).map {
                Conversation(
                    id = it.id,
                    name = it.name,
                    lastModified = it.lastModified,
                    messageCount = it.messageCount
                )
            }
        } catch (e: Exception) {
            return@get call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
        call.respond(ConversationHistory(conversations))
    }
    get("$GET/{conversationId}") {
        try {
            val conversationId = call.parameters["conversationId"] ?: return@get call.respondText(
                "Missing conversation ID",
                status = HttpStatusCode.BadRequest
            )

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val messages = getMessageHistory(conversationId, limit)
            call.respond(messages)
        } catch (e: Exception) {
            return@get call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}
