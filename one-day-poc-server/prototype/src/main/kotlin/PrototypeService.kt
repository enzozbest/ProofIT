package kcl.seg.rtt.prototype

import kotlinx.serialization.Serializable

/**
 * Represents a structured response from the LLM containing prototype file information.
 *
 * @property mainFile The entry point file for the prototype (e.g., "index.js")
 * @property files A map of filenames to their contents, representing the complete
 *                prototype file structure
 */
@Serializable
data class LlmResponse(
    val mainFile: String,
    val files: Map<String, FileContent>
)

@Serializable
data class FileContent(val content: String)


open class PrototypeService(private val ollamaService: OllamaService) {

    /**
     * Generates a software prototype using Ollama LLM
     *
     * @param prompt User requirements for the prototype
     * @return Result containing prototype structure or failure with error details
     */
    open suspend fun generatePrototype(prompt: String): Result<LlmResponse> {
        val fullPrompt = createPrompt(prompt)
        return ollamaService.generateResponse(fullPrompt)
    }

    /**
     * Creates a prompt combining user input with system instructions for WebContainers format
     *
     * @param userPrompt Original user input
     * @return Formatted prompt with system instructions
     */
    private fun createPrompt(userPrompt: String): String {
        return """
            You are an AI that generates software prototypes formatted for WebContainers.  
            Your response must be **a single valid JSON object** and contain nothing else—no explanations, preambles, or additional text.
            
            If you are creating a multi page website, please provide only one file with all HTML CSS and JS
            Different pages must be represented by different divs with a class of "page" and only one div with a class of "active"

            ### JSON Structure:
            - `"mainFile"`: Specifies the main entry file (e.g., `"index.js"`).
            - `"files"`: An object where each key is a filename and the value is an object containing:
            - `"content"`: The full content of the file.
            - `"package.json"`: Must be included with all required dependencies.
            - Ensure that:
                - All scripts use `"npm start"` for execution.
                - Static files (if any) are served correctly.

            Now, generate a JSON response for the following request:

            **User Request:**  
            "$userPrompt"
        """.trimIndent()
    }

    open fun retrievePrototype(id: String): String {
        // Later, this will query the DB or S3, etc.
        // For now, just return a minimal HTML snippet (or null).
        return "<html><body><h1>Hello from Prototype $id</h1></body></html>"
    }

}