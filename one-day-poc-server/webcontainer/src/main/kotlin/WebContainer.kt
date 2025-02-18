package kcl.seg.rtt.webcontainer

import kotlinx.serialization.Serializable
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.routing.*


// Data class to hold prototype content
// Can change to relevant languages
@Serializable
data class WebContainerContent(
    val html: String,
    val css: String,
    val js: String
)

@Serializable
class WebContainer {


    /**
     * Parses the prototype content from the given [prototypeString].
     *
     * @param prototypeString The string to parse.
     * @return The parsed [WebContainerContent].
     */
    private fun parsePrototype(prototypeString: String) : String{
        // Change based off our LLM Response
        return prototypeString
    }

    /**
     * Attempts to retrieve the "id" parameter from [call].
     * If missing or blank, responds with [HttpStatusCode.BadRequest] and returns `null`.
     *
     * @param call The [ApplicationCall] for the current request.
     * @param errorMessage Optional custom error message for missing/blank ID.
     * @return The ID string if valid, otherwise `null` (and an HTTP 400 response is sent).
     */
    suspend fun getValidPrototypeIdOrRespond(call: ApplicationCall, errorMessage: String = "Missing ID."): String? {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, errorMessage)
            return null
        }
        return id
    }

    /**
     * Defines the routes for the WebContainer.
     *
     * @see getValidPrototypeIdOrRespond
     */
    fun Route.webcontainerRoutes() {
        get("/webcontainer/{id}") {
            // Reuse the ID validator function
            val prototypeId = getValidPrototypeIdOrRespond(call) ?: return@get

            val content = "<html><body><h1>Hello from Prototype $prototypeId</h1></body></html>"
            parsePrototype(content)
            if (content.isNullOrEmpty()) {
                call.respond(HttpStatusCode.NotFound, "No content for $prototypeId")
            } else {
                call.respondText(content, ContentType.Text.Html)
            }
        }
    }
}

// Extension function for Application
fun Application.configureWebContainer() {

    routing {
        val webContainer = WebContainer()
        with(webContainer) { webcontainerRoutes() }

    }
}


