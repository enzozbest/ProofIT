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
    val rawLLMText: String? = null
)

@Serializable
data class FileContent(val content: String)


open class PrototypeService(private val ollamaService: OllamaService) {

    /**
     * Generates a software prototype using the LLM service [ollamaService] based on the given [prompt].
     *
     * Steps:
     * 1. Creates a full prompt string via [createPrompt].
     * 2. Calls [ollamaService.generateResponse] with that prompt, returning an [LlmResponse] wrapped in [Result].
     * 3. Checks if the generated response contains a "new template" declaration:
     *    - If so, validates the template with [TemplateManager.validateNewTemplate].
     *    - On successful validation, calls [TemplateManager.storeNewTemplateEmbeddings].
     * 4. Performs a fake compilation check with [runCompileCheckOnSite] to ensure the final code/site is valid.
     *
     * If any checks fail, this function returns a failed [Result] with a relevant [RuntimeException],
     * allowing the caller to prompt the LLM to regenerate or handle the error further.
     *
     * @param prompt The raw user prompt specifying what the LLM should generate.
     * @return A [Result] containing an [LlmResponse] if all validations succeed, or a failed [Result]
     *         if any validation step fails (e.g., new template invalid, code compile error).
     */
    suspend fun generatePrototype(prompt: String): Result<LlmResponse> {
        val fullPrompt = createPrompt(prompt)
        val llmResult = ollamaService.generateResponse(fullPrompt)

        val rawText = llmResult.getOrNull()?.rawLLMText?: "" // or something appropriate
        if (TemplateManager.detectNewTemplate(rawText)) {
            val validation = TemplateManager.validateNewTemplate(rawText)
            if (!validation.success) {
                // fail the result so the LLM can regenerate
                return Result.failure(RuntimeException("Template validation failed: ${validation.errorMessage}"))
            }
            // if success, store embeddings
            TemplateManager.storeNewTemplateEmbeddings(rawText)
        }

        // Also validate the "regular" code or site portion
        val compiledOk = runCompileCheckOnSite(llmResult.getOrNull())
        if (!compiledOk) {
            return Result.failure(RuntimeException("Site code failed to compile or run. Regenerate needed."))
        }


        return llmResult
    }

    /**
     * Performs a naive "compile" or validation check on the files within the given [response].
     *
     * Currently, this method:
     * 1. Returns false if [response] is null.
     * 2. Inspects each file in [response.files], failing if any file content contains "ERROR" (case-insensitive).
     *
     * @param response The [LlmResponse] to be checked. May be null, in which case it fails immediately.
     * @return True if the response is non-null and no file contains "ERROR"; false otherwise.
     */
    private fun runCompileCheckOnSite(response: LlmResponse?): Boolean {
        // TODO: parse the "files" in LlmResponse and run some checks.
        if (response == null) return false

        // naive approach
        return !response.files.values.any { it.content.contains("ERROR", ignoreCase = true) }
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