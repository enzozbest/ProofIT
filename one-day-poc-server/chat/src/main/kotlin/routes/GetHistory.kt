package kcl.seg.rtt.chat.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*
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