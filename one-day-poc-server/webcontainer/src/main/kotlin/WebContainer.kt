package kcl.seg.rtt.webcontainer

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kcl.seg.rtt.prototype.LlmResponse

// Add this singleton object to store the latest response
object WebContainerState {
    private var latestResponse: LlmResponse? = null
    
    fun updateResponse(response: LlmResponse) {
        latestResponse = response
    }
    
    fun getLatestResponse(): LlmResponse? = latestResponse
}

/**
 * Defines routes to serve code snippets from an [LlmResponse]
 * at /prototype/<language>.
 */
fun Route.parseCode(llmResponse: LlmResponse) {
    // For each language key in llmResponse.files, set up a GET route
    llmResponse.files.forEach { (language, fileContent) ->
        get("/prototype/$language") {
            val codeSnippet = fileContent.content
            // Determine ContentType based on language
            val contentType = when (language.lowercase()) {
                "html" -> ContentType.Text.Html
                "css" -> ContentType.Text.CSS
                "js", "javascript" -> ContentType.Text.JavaScript
                else -> ContentType.Text.Plain  // python, etc. served as text
            }

            call.respondText(codeSnippet, contentType)
        }
    }
}

// Extension function for Application
fun Application.configureWebContainer() {
    routing {
        route("/prototype") {
            get("/{language}") {
                val language = call.parameters["language"] ?: return@get call.respondText(
                    "Missing language parameter", 
                    status = HttpStatusCode.BadRequest
                )
                
                val response = WebContainerState.getLatestResponse()
                if (response == null) {
                    call.respondText(
                        "No prototype available yet", 
                        status = HttpStatusCode.NotFound
                    )
                    return@get
                }
                
                val fileContent = response.files[language]
                if (fileContent == null) {
                    call.respondText(
                        "No content available for language: $language", 
                        status = HttpStatusCode.NotFound
                    )
                    return@get
                }
                
                // Determine ContentType based on language
                val contentType = when (language.lowercase()) {
                    "html" -> ContentType.Text.Html
                    "css" -> ContentType.Text.CSS
                    "js", "javascript" -> ContentType.Text.JavaScript
                    else -> ContentType.Text.Plain
                }
                
                call.respondText(fileContent.content, contentType)
            }
        }
    }
}