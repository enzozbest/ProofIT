package kcl.seg.rtt.webcontainer

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kcl.seg.rtt.prototype.LlmResponse
import kcl.seg.rtt.prototype.FileContent
import kotlinx.serialization.json.JsonObject

// Hardcoded website content for testing
private val TEST_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Simple Counter</title>
    <link rel="stylesheet" href="/prototype/css">
</head>
<body>
    <div class="container">
        <h1>Counter</h1>
        <div class="counter-display">
            <span id="count">0</span>
        </div>
        <div class="buttons">
            <button id="decrement">-</button>
            <button id="reset">Reset</button>
            <button id="increment">+</button>
        </div>
    </div>
    <script src="/prototype/js"></script>
</body>
</html>
""".trimIndent()

private val TEST_CSS = """
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    font-family: 'Arial', sans-serif;
}

body {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background-color: #f5f5f5;
}

.container {
    background-color: white;
    border-radius: 10px;
    padding: 30px;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    text-align: center;
    width: 300px;
}

h1 {
    color: #333;
    margin-bottom: 20px;
}

.counter-display {
    font-size: 72px;
    font-weight: bold;
    color: #333;
    margin: 20px 0;
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.buttons {
    display: flex;
    justify-content: space-between;
}

button {
    padding: 10px 20px;
    font-size: 24px;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.3s;
}

#decrement {
    background-color: #ff6347;
    color: white;
}

#reset {
    background-color: #4d4d4d;
    color: white;
}

#increment {
    background-color: #4caf50;
    color: white;
}

button:hover {
    opacity: 0.9;
}

button:active {
    transform: scale(0.98);
}
""".trimIndent()

private val TEST_JS = """
// Initialize counter value
let count = 0;

// Get DOM elements
const countDisplay = document.getElementById('count');
const incrementBtn = document.getElementById('increment');
const decrementBtn = document.getElementById('decrement');
const resetBtn = document.getElementById('reset');

// Update display function
function updateDisplay() {
    countDisplay.textContent = count;
    
    // Change color based on value
    if (count > 0) {
        countDisplay.style.color = '#4caf50';
    } else if (count < 0) {
        countDisplay.style.color = '#ff6347';
    } else {
        countDisplay.style.color = '#333';
    }
}

// Event listeners
incrementBtn.addEventListener('click', () => {
    count++;
    updateDisplay();
});

decrementBtn.addEventListener('click', () => {
    count--;
    updateDisplay();
});

resetBtn.addEventListener('click', () => {
    count = 0;
    updateDisplay();
});

// Initial display update
updateDisplay();
""".trimIndent()

// Create test response with the hardcoded website
private val TEST_RESPONSE = LlmResponse(
    mainFile = "index.html",
    files = mapOf(
        "html" to FileContent(TEST_HTML),
        "css" to FileContent(TEST_CSS),
        "js" to FileContent(TEST_JS)
    )
)

// Modified singleton to use hardcoded response by default
object WebContainerState {
    private var latestResponse: LlmResponse? = TEST_RESPONSE
    
    fun updateResponse(response: LlmResponse) {
        latestResponse = response
    }
    
    // fun getLatestResponse(): LlmResponse? = latestResponse
    fun getLatestResponse(): LlmResponse = TEST_RESPONSE
    
    // Reset to test response for testing
    fun resetToTestResponse() {
        latestResponse = TEST_RESPONSE
    }
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

// Add a separate function for root redirect to ensure it's registered first
fun Application.configureRootRedirect() {
    routing {
        get("/") {
            log.info("Root path handler called")
            val response = WebContainerState.getLatestResponse()
            if (response != null && response.files.containsKey("html")) {
                log.info("Redirecting to /prototype/html")
                call.respondRedirect("/prototype/html")
            } else {
                call.respondText("No prototype generated yet", status = HttpStatusCode.NotFound)
            }
        }
    }
}

// Extension function for Application
fun Application.configureWebContainer() {
    log.info("Configuring WebContainer routes") // Add logging
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