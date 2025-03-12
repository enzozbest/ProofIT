package chat

import chat.routes.chatRoutes
import chat.routes.jsonRoutes
import chat.routes.uploadRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

const val CHAT = "/api/chat"
const val GET = "$CHAT/get"
const val JSON = "$CHAT/json"
const val UPLOAD = "$CHAT/upload"

object ChatEndpoint {
    internal var UPLOAD_DIR: String = "uploads" // Do not use val for testing!
}

/**
 * Configures the chat module with authenticated routes.
 * 
 * This function sets up all chat-related endpoints with JWT authentication.
 * It registers routes for chat interactions, JSON processing, and file uploads.
 * 
 * @receiver The Application in which the module is installed
 */
fun Application.chatModule() {
    routing {
        authenticate("jwt-verifier") {
            chatRoutes()
            jsonRoutes()
            uploadRoutes(ChatEndpoint.UPLOAD_DIR)
        }
    }
}
