package kcl.seg.rtt.webcontainer

import kotlinx.serialization.Serializable
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

@Serializable
class WebContainer {

    fun parsePrototype(prototypeString: String): WebContainerContent {
        // CHange based off our prototype format
        return WebContainerContent(
            html = "<h1>Hello, Prototype!</h1>",  // For example purposes
            css = "body { background-color: #fafafa; }",
            js = "console.log('Prototype loaded');"
        )
    }

    // Set up routes for webcontainer
    fun Route.webcontainerRoutes() {
        get("/webcontainer/{id}") {
            // (You can still use the prototypeId if needed)
            val htmlResponse = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Multi-Page Prototype</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        text-align: center;
                    }
                    .page { display: none; }
                    .active { display: block; }
                    button {
                        padding: 10px 20px;
                        font-size: 16px;
                        margin: 10px;
                        cursor: pointer;
                    }
                </style>
            </head>
            <body>
                <div id="counterPage" class="page active">
                    <p id="counterText">Button clicked 0 times!</p>
                    <button id="incrementButton">Increment Counter</button>
                    <button id="goToHiButton">Go to Hi Page</button>
                </div>
                <div id="hiPage" class="page">
                    <h1>Hi Prototype!</h1>
                    <button id="backToCounterButton">Back to Counter</button>
                </div>
                <script>
                    let counter = 0;
                    const counterText = document.getElementById('counterText');
                    const incrementButton = document.getElementById('incrementButton');
                    const goToHiButton = document.getElementById('goToHiButton');
                    const backToCounterButton = document.getElementById('backToCounterButton');

                    incrementButton.addEventListener('click', () => {
                        counter++;
                        counterText.textContent = 'Button clicked ' + counter + ' times!';
                    });

                    goToHiButton.addEventListener('click', () => {
                        showPage('hiPage');
                    });

                    backToCounterButton.addEventListener('click', () => {
                        showPage('counterPage');
                    });

                    function showPage(pageId) {
                        document.querySelectorAll('.page').forEach(page => {
                            page.classList.remove('active');
                        });
                        document.getElementById(pageId).classList.add('active');
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

            call.respondText(htmlResponse, ContentType.Text.Html)
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