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
 */
fun Route.jsonRoutes() {
    post(JSON) {
        try {
            val request = call.receive<Request>()
            println("Received request: $request")
            val prompt = cleanPrompt(request.prompt)
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

/*
    * User prompts via the JSON prompt request are sanitised by
    * removing all HTML tags (Jsoup)
    * removing leading and trailing whitespace
    * replacing special characters and HTML entities such as &lt;
    * with the empty string
    * capping the user input to 1000 characters
    * ignoring all text after a word/phrase if the prompt contains a malicious phrase
    *
 */
private fun cleanPrompt(prompt: String): String {
    var sanitised = Jsoup.clean(prompt, Safelist.none())
        .trim()
        .replace(Regex("&[a-zA-Z0-9#]+;"), "")
        .replace(Regex("[^\\w\\s.,!?()]"), "")
        .take(1000)
    val maliciousPatterns = listOf(
    "ignore", "pretend", "disregard", "act like", "follow my instructions",
        "do not follow", "override", "act as", "respond as"
    )
    for (pattern in maliciousPatterns) {
        val regex = Regex("((?i)$pattern)")
        sanitised = sanitised.replace(regex, "")
    }
    return sanitised
}