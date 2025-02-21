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
            val prompt = processPrompt(request.prompt)
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

//For now, just return the sanitised prompt,later we can decide about the keywords
private fun processPrompt(prompt: String):String {
    val sanitisedPrompt = cleanPrompt(prompt)
    val keywords = extractKeywords(sanitisedPrompt)
    println("Keywords are: $keywords")
    return sanitisedPrompt
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

/*
    * This method extracts keywords from the user prompt submitted
    * These words will be added to a list and eventually passed to the llm
    * with the sanitised prompt
    *
    * The keywords can later be expanded when our use cases expand
 */
private fun extractKeywords(prompt: String): List<String> {
    val keywords = listOf(
        "javascript","html","css","chatbot","chat bot","button","report",
        "ai","assistant","generate","generation","website","webpage","page",
        "application","web","frontend","backend","ui","interface","database"
    )
    val usedKeywords = mutableListOf<String>()
    val lowercasePrompt = prompt.lowercase()
    for (keyword in keywords){
        if (keyword in lowercasePrompt){
            usedKeywords.add(keyword)
        }
    }
    return usedKeywords
}