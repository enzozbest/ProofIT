package chat.routes

import chat.GET
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/*
 * This is a placeholder for the chat history route.
 */
internal fun Route.chatRoutes() {
    get(GET) {
        call.respondText("Hello, world!")
        // once linked to s3 bucket this will return chat history
    }
}
