package prototype

import kotlinx.serialization.json.JsonObject
import java.io.File
import prompting.ServerResponse
import prompting.ChatResponse
import prompting.PrototypeResponse
import kotlinx.serialization.json.Json
import java.time.Instant


/**
 * Data class representing a template loaded from a JSON file.
 */
@kotlinx.serialization.Serializable
data class PredefinedPrototype(
    val name: String,
    val description: String,
    val message: String,
    val keywords: List<String>,
    val files: JsonObject
)

/**
 * Service for loading and providing pre-defined prototypes.
 *
 * This service loads pre-defined prototypes from JSON files and provides
 * them based on prompt content.
 */
object PredefinedPrototypeService {
    private const val PROTOTYPES_DIR = "src/main/resources/prototypes"

    /**
     * Gets a prototype response for a given prompt.
     *
     * @param prompt The user's input prompt
     * @return A ServerResponse containing the appropriate prototype
     */
    fun getPrototypeForPrompt(prompt: String): ServerResponse {
        val normalizedPrompt = prompt.lowercase()
        val prototypeDir = File(PROTOTYPES_DIR)

        if (!prototypeDir.exists() || !prototypeDir.isDirectory) {
            println("Warning: Prototypes directory not found at $PROTOTYPES_DIR")
            return createFailedResponse(prompt)
        }

        val prototypeFiles = prototypeDir.listFiles { file -> file.extension == "json" } ?: emptyArray()

        // Try to find exact filename match
        for (file in prototypeFiles) {
            val filenameWithoutExtension = file.nameWithoutExtension.lowercase()

            // If the filename is directly contained in the prompt, use this template
            if (normalizedPrompt.contains(filenameWithoutExtension)) {
                return loadAndCreateResponse(file, prompt)
            }
        }


        // No matching template found, return response indicating failure
        return createFailedResponse(prompt)
    }

    /**
     * Loads a prototype file and creates a response from it.
     *
     * @param file The template file to load
     * @param originalPrompt The original user prompt (for error handling)
     * @return A ServerResponse based on the template file
     */
    private fun loadAndCreateResponse(file: File, originalPrompt: String): ServerResponse {
        return try {
            val content = file.readText()
            val template = Json.decodeFromString<PredefinedPrototype>(content)

            ServerResponse(
                chat = ChatResponse(
                    message = template.message,
                    role = "LLM",
                    timestamp = Instant.now().toString(),
                    messageId = "0"
                ),
                prototype = PrototypeResponse(
                    files = template.files
                )
            )
        } catch (e: Exception) {
            println("Error loading template ${file.name}: ${e.message}")
            createFailedResponse(originalPrompt)
        }
    }

    private fun createFailedResponse(originalPrompt: String): ServerResponse {
        return ServerResponse(
            chat = ChatResponse(
                message = "We don't have a pre-defined prototype for the inserted prompt: $originalPrompt",
                role = "LLM",
                timestamp = Instant.now().toString(),
                messageId = "0",
            ),
            prototype = null
        )
    }


}