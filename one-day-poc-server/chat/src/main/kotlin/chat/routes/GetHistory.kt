package chat.routes

import chat.GET
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import chat.storage.*
import io.ktor.server.request.*

@Serializable
data class Conversation(
    val id: String,
    val name: String,
    val lastModified: String,
    val messageCount: Int,
    val userId: String
)

@Serializable
data class ConversationHistory(
    val conversations: List<Conversation>
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val timestamp: String
)

internal fun Route.chatRoutes() {
    get(GET) {
        try {
            println("Fetching conversations")
            val userId = call.request.queryParameters["userId"] ?: "user" 

            val conversations = getConversationHistory(userId).map {
                Conversation(
                    id = it.id,
                    name = it.name,
                    lastModified = it.lastModified,
                    messageCount = it.messageCount,
                    userId = it.userId
                )
            }
            println("Fetched ${conversations.size} conversations")
            call.respond(ConversationHistory(conversations))
        } catch (e: Exception) {
            return@get call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
    get("$GET/{conversationId}") {
        try {
            val conversationId = call.parameters["conversationId"] ?: return@get call.respondText(
                "Missing conversation ID",
                status = HttpStatusCode.BadRequest
            )

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            println("Fetching messages")
            val messages = getMessageHistory(conversationId, limit)
            println("Fetched $messages messages")
            
            val messageDtos = messages.map { message ->
                MessageDto(
                    id = message.id,
                    conversationId = message.conversationId,
                    senderId = message.senderId,
                    content = message.content,
                    timestamp = message.timestamp.toString()
                )
            }
            
            call.respond(messageDtos)
        } catch (e: Exception) {
            println("Error getting messages: ${e.message}")
            e.printStackTrace()
            return@get call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}
