package kcl.seg.rtt.chat_history.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kcl.seg.rtt.chat_history.Request
import kcl.seg.rtt.chat_history.Response
import java.time.LocalDateTime
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist


/*
    * This route is used to handle JSON requests with the Request.kt schema
 */
fun Route.jsonRoutes() {
    post("/json") {
        try {
            val request = call.receive<Request>()
            println("Received request: $request")
            val prompt = Jsoup.clean(request.prompt, Safelist.none()).trim().replace(Regex("&[a-zA-Z0-9#]+;"), "")
            val response = Response(
                time = LocalDateTime.now().toString(),
                message = "${prompt}, ${request.userID}!")
            println(prompt)
            call.respond(response)

        } catch (e: Exception) {
            call.respondText(
                text = "Invalid request: ${e.message}",
                status = HttpStatusCode.BadRequest
            )
        }
    }
}