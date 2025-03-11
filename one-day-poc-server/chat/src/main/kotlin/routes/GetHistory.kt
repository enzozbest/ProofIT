package kcl.seg.rtt.chat.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kcl.seg.rtt.chat.GET

/*
 * This is a placeholder for the chat history route.
 */
fun Route.chatRoutes() {
    get(GET) {
        call.respondText("Hello, world!")
        // once linked to s3 bucket this will return chat history
    }
}
