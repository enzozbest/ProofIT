package prototype

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import prototype.helpers.PromptException

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

/**
 * Converts a JsonObject to an LlmResponse object for security checking.
 *
 * @param json The JsonObject containing the LLM response with files
 * @return An LlmResponse object containing the parsed files
 * @throws PromptException If the required fields are missing or malformed
 */
fun convertJsonToLlmResponse(json: JsonObject): LlmResponse {
    try {
        // Convert each file entry to a map of language -> FileContent
        val files = mutableMapOf<String, FileContent>()

        (
            json["files"] as? JsonObject
                ?: throw PromptException("Missing 'files' field in LLM response")
        ).entries.forEach { (language, fileContentJson) ->
            val content =
                when (fileContentJson) {
                    is JsonObject -> {
                        // Look for "code" field first (as per prompt), then try "content" for backward compatibility
                        (fileContentJson["code"] as? JsonPrimitive)?.content
                            ?: (fileContentJson["content"] as? JsonPrimitive)?.content
                            ?: throw PromptException("Missing 'code' or 'content' field in file for language: $language")
                    }

                    is JsonPrimitive -> {
                        // If it's directly a string content
                        fileContentJson.content
                    }

                    else -> throw PromptException("Unexpected format for file content in language: $language")
                }

            files[language] = FileContent(content)
        }

        // Set default mainFile to "index.html" if not provided
        val mainFile = (json["mainFile"] as? JsonPrimitive)?.content ?: "html"
        return LlmResponse(mainFile, files)
    } catch (e: Exception) {
        when (e) {
            is PromptException -> throw e
            else -> throw PromptException("Failed to parse LLM response: ${e.message}")
        }
    }
}
