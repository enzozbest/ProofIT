package kcl.seg.rtt.chat_history.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kcl.seg.rtt.chat_history.JSON
import kcl.seg.rtt.chat_history.Request
import kcl.seg.rtt.chat_history.Response
import java.time.LocalDateTime
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/*
    * This route is used to handle JSON requests with the Request.kt schema
    *
    * User prompts via the JSON prompt request are sanitised by
    *           removing all HTML tags (Jsoup)
    *           removing leading and trailing whitespace
    *           replacing HTML entities such as &lt; with the empty string
 */
fun Route.jsonRoutes() {
    post(JSON) {
        try {
            val request = call.receive<Request>()
            println("Received request: $request")
            val prompt = Jsoup.clean(request.prompt, Safelist.none())
                .trim()
                .replace(Regex("&[a-zA-Z0-9#]+;"), "")
                .replace(Regex("[^\\w\\s.,!?()]"), "")
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