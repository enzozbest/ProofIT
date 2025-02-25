package kcl.seg.rtt.chat.routes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kcl.seg.rtt.chat.JSON
import kcl.seg.rtt.chat.Request
import kcl.seg.rtt.chat.Response
import kcl.seg.rtt.chat.utils.KeywordLoader
import java.time.LocalDateTime
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

//private val client = HttpClient(CIO)
//private val HOST = "http://localhost:8000"
//private val ENDPOINT = "$HOST/api/prototype/generate"

private var client = HttpClient(CIO)
private var ENDPOINT = "http://localhost:8000/api/prototype/generate"

internal fun setTestClient(testClient: HttpClient) {
    client = testClient
}

internal fun setTestEndpoint(endpoint: String) {
    ENDPOINT = endpoint
}

fun Route.jsonRoutes() {
    post(JSON) {
        try {
            val request = call.receive<Request>()
            handleJsonRequest(request, call)
        } catch (e: Exception) {
            handleError(e, call)
        }
    }
}

private suspend fun handleJsonRequest(request: Request, call: ApplicationCall) {
    println("Received request: $request")
    val cleanPrompt = sanitisePrompt(request.prompt)
    val prototypeResponse = makePrototypeRequest(cleanPrompt)
    handlePrototypeResponse(prototypeResponse, call)
}

//For now, just return the sanitised prompt,later we can decide about the keywords
internal fun sanitisePrompt(prompt: String):String {
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
 */
private fun extractKeywords(prompt: String): List<String> {
    val keywordSet = KeywordLoader.getKeywordsList().toSet()
    val lowercasePrompt = prompt.lowercase()
    return keywordSet.filter { it in lowercasePrompt }
}

private suspend fun makePrototypeRequest(prompt: String): HttpResponse {
    return client.post(ENDPOINT) {
        contentType(ContentType.Application.Json)
        setBody("""{"prompt": "$prompt"}""")
    }
}

private suspend fun handlePrototypeResponse(
    prototypeResponse: HttpResponse,
    call: ApplicationCall
) {
    if (prototypeResponse.status.isSuccess()) {
        handleSuccessResponse(prototypeResponse, call)
    } else {
        handleFailureResponse(prototypeResponse, call)
    }
}

private suspend fun handleSuccessResponse(
    prototypeResponse: HttpResponse,
    call: ApplicationCall
) {
    val response = createResponse(prototypeResponse.bodyAsText())
    call.respond(response)
}

private suspend fun handleFailureResponse(
    prototypeResponse: HttpResponse,
    call: ApplicationCall
) {
//                call.respondText(
//                    text = "Prototype service error: ${prototypeResponse.status}",
//                    status = HttpStatusCode.InternalServerError
//                )
    println("Error: ${prototypeResponse.status}")
    val response = createResponse("Error: ${prototypeResponse.status}, ${prototypeResponse.bodyAsText()}")
    call.respond(response)
}

private fun createResponse(message: String): Response {
    return Response(
        time = LocalDateTime.now().toString(),
        message = message
    )
}

private suspend fun handleError(e: Exception, call: ApplicationCall) {
    call.respondText(
        text = "Invalid request: ${e.message}",
        status = HttpStatusCode.BadRequest
    )
}