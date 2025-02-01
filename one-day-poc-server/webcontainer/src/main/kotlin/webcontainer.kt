package kcl.seg.rtt.webcontainer

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.routing.*

// Data class to hold prototype content
// Can change to relevant languages
data class WebContainerContent(
    val html: String,
    val css: String,
    val js: String
)

class WebContainer {
    // Convert prototype string to WebContainer format
    fun parsePrototype(prototypeString: String): WebContainerContent {
        // This will need to be implemented based on how your LLM
        // structures the prototype string
        return WebContainerContent(
            html = "", // Extract HTML content
            css = "",  // Extract CSS content
            js = ""    // Extract JavaScript content
        )
    }

    // Set up routes for webcontainer
    fun Route.webcontainerRoutes() {
        // Enable CORS for iframe access
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
        }
        get("/webcontainer/{id}") {
            val prototypeId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing prototype ID"
            )

            // This will need to integrate with PrototypeService
            // to fetch the prototype string
            val prototypeString = "PrototypeResult" // Get from PrototypeRoutes

            val content = parsePrototype(prototypeString)

            call.respond(content)
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

