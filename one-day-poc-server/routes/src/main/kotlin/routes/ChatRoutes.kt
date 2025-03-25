package routes

import chat.ChatEndpoints.setChatRoutes
import chat.ChatEndpoints.setJsonRoutes
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

object ChatRoutes {
    /**
     * Configures the chat module with authenticated routes.
     *
     * This function sets up all chat-related endpoints with JWT authentication.
     * It registers routes for chat interactions, JSON processing, and file uploads.
     *
     * @receiver The Application in which the module is installed
     */
    fun Application.configureChatRoutes() =
        routing {
            authenticate("jwt-verifier") {
                setChatRoutes()
                setJsonRoutes()
            }
        }
}
