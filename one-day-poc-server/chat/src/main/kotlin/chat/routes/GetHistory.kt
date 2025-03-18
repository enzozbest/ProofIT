package chat.routes

import chat.GET
import io.ktor.http.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import java.time.ZoneOffset
import chat.storage.ChatStorageFactory

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
        val conversationIds = mutableSetOf<String>()
        val storage = ChatStorageFactory.getStorage(true)

        val userId = call.request.queryParameters["userId"] ?: "user"
        val userMessages = storage.getMessagesByUser(userId, 1000, 0)

        userMessages.forEach {
            conversationIds.add(it.conversationId)
        }

        val conversations = conversationIds.map { conversationId ->
            val firstMessage = storage.getMessagesByConversation(conversationId, 1, 0).firstOrNull()
            val messageCount = storage.getConversationMessageCount(conversationId)
            val lastMessage = storage.getMessagesByConversation(conversationId, 1, 0, sortDescending = true).firstOrNull()
            Conversation(
                id = conversationId,
                name = firstMessage?.content?.take(30)?.plus("...") ?: "New Conversation",
                lastModified = lastMessage?.timestamp?.atOffset(ZoneOffset.UTC)?.toString() ?: "",
                messageCount = messageCount
            )
        }.sortedByDescending { it.lastModified }

        call.respond(ConversationHistory(conversations))
    }
    get("$GET/{conversationId}") {
        val conversationId = call.parameters["conversationId"] ?: return@get call.respondText(
            "Missing conversation ID",
            status = HttpStatusCode.BadRequest
        )

        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

        val messages = getMessageHistory(conversationId, limit, true)
        call.respond(messages)
    }
}
