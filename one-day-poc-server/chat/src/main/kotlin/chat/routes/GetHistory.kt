package chat.routes

import chat.GET
import chat.storage.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String,
    val name: String,
    val lastModified: String,
    val messageCount: Int,
    val userId: String,
)

@Serializable
data class ConversationHistory(
    val conversations: List<Conversation>,
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val timestamp: String,
)

@Serializable
data class PrototypeDto(
    val files: String,
)

internal fun Route.setGetHistoryRoute() {
    get(GET) {
        try {
            println("Fetching conversations")
            val userId = call.request.queryParameters["userId"] ?: "user"

            val conversations =
                getConversationHistory(userId).map {
                    Conversation(
                        id = it.id,
                        name = it.name,
                        lastModified = it.lastModified,
                        messageCount = it.messageCount,
                        userId = it.userId,
                    )
                }
            println("Fetched ${conversations.size} conversations")
            call.respond(ConversationHistory(conversations))
        } catch (e: Exception) {
            return@get call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}

internal fun Route.setGetConversationRoute() {
    get("$GET//msg1") {
        return@get call.respondText(
            "Empty conversation ID",
            status = HttpStatusCode.NotFound,
        )
    }

    get("$GET/{conversationId}") {
        try {
            val conversationId = call.parameters["conversationId"]!!

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            println("Fetching messages")
            val messages = getMessageHistory(conversationId, limit, offset)
            println("Fetched $messages messages")

            val messageDtos =
                messages.map { message ->
                    MessageDto(
                        id = message.id,
                        conversationId = message.conversationId,
                        senderId = message.senderId,
                        content = message.content,
                        timestamp = message.timestamp.toString(),
                    )
                }

            call.respond(messageDtos)
        } catch (e: Exception) {
            println("Error getting messages: ${e.message}")
            e.printStackTrace()
            return@get call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}

internal fun Route.setGetPrototypeRoute() {
    get("$GET/{conversationId}/{messageId}") {
        try {
            println("Fetching prototype")
            val conversationId = call.parameters["conversationId"]!!

            val messageId = call.parameters["messageId"]!!
            val prototype = retrievePrototype(conversationId, messageId)

            if (prototype != null) {
                call.respond(PrototypeDto(files = prototype.filesJson))
            } else {
                call.respondText(
                    "Prototype not found",
                    status = HttpStatusCode.NotFound,
                )
            }
        } catch (e: Exception) {
            println("Error getting messages: ${e.message}")
            e.printStackTrace()
            return@get call.respondText(
                "Error: ${e.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}
