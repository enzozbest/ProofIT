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
    val files: Map<String, FileContent>,
)

@Serializable
data class FileContent(
    val content: String,
)

open class PrototypeService(
    private val ollamaService: OllamaService,
) {
    /**
     * Generates a software prototype using Ollama LLM
     *
     * @param prompt User requirements for the prototype
     * @return Result containing prototype structure or failure with error details
     */
    /*    open suspend fun generatePrototype(
            prompt: String,
            keywords: List<String>,
        ): Result<LlmResponse> {
            val placeholder = OllamaRequest("deepseek-r1:32b", prompt, false) // !!!!///
            val response = ollamaService.generateResponse(placeholder)
            var requirements = ""
            if (response.isSuccess) {
                val llmResponse = response.getOrThrow()
                requirements = llmResponse.mainFile
            }
            //val fullPrompt = createPrompt(requirements)
            //return ollamaService.generateResponse(fullPrompt)
        }*/

    open fun retrievePrototype(id: String): String {
        // Later, this will query the DB or S3, etc.
        // For now, just return a minimal HTML snippet (or null).
        return "<html><body><h1>Hello from Prototype $id</h1></body></html>"
    }
}
