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

    open fun retrievePrototype(id: String): String {
        // Later, this will query the DB or S3, etc.
        // For now, just return a minimal HTML snippet (or null).
        return "<html><body><h1>Hello from Prototype $id</h1></body></html>"
    }
}
