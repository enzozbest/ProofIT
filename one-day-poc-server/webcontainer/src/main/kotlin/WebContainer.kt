package kcl.seg.rtt.webcontainer

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*

// Data class to hold prototype content
// Can change to relevant languages
@Serializable
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

        get("/webcontainer/{id}") {
            val prototypeId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing prototype ID"
            )

            // This will need to integrate with PrototypeService
            // to fetch the prototype string
            val prototypeString = "PrototypeResult" // Get from PrototypeRoutes

            //val content = parsePrototype(prototypeString)

            val content = "this is a prototype string"

            call.respond(content)
        }
    }
}

// Extension function for Application
fun Application.configureWebContainer() {
    // enable CORS for iframe access
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    routing {
        val webContainer = WebContainer()
        with(webContainer) { webcontainerRoutes() }
    }
}

/**
 * TO BE COPIED TO FRONTEND UPON MERGE FOR WEBCONTAINER
 * (iframe code)
 *
 * import React, { useEffect, useState } from 'react';
 *
 * interface PrototypeFrameProps {
 *     prototypeId: string;
 * }
 *
 * const PrototypeFrame: React.FC<PrototypeFrameProps> = ({ prototypeId }) => {
 *     const [url, setUrl] = useState('');
 *
 *     useEffect(() => {
 *         setUrl(`http://localhost:8080/webcontainer/${prototypeId}`);
 *     }, [prototypeId]);
 *
 *     return (
 *         <iframe
 *             src={url}
 *             style={{
 *                 width: '100%',
 *                 height: '500px',
 *                 border: '1px solid #ccc',
 *                 borderRadius: '4px'
 *             }}
 *             title="Prototype Preview"
 *             sandbox="allow-scripts allow-same-origin"
 *         />
 *     );
 * };
 *
 * export default PrototypeFrame;
 *
 *
 */