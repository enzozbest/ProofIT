package prototype

import kotlinx.serialization.json.JsonObject
import java.io.File
import kotlinx.serialization.json.Json
import java.net.URL


/**
 * Data class representing a template loaded from a JSON file.
 */
@kotlinx.serialization.Serializable
data class PredefinedPrototype(
    val message: String,
    val keywords: List<String>,
    val files: JsonObject
)

/**
 * Simple data class representing the raw content from a predefined prototype template.
 * This allows JsonRoutes to handle the database persistence and response construction.
 */
data class PrototypeTemplate(
    val chatMessage: String,
    val files: JsonObject,
)

/**
 * Service for loading and providing pre-defined prototypes.
 *
 * This service loads pre-defined prototypes from JSON files and provides
 * them based on prompt content.
 */
object PredefinedPrototypeService {
    private const val PROTOTYPES_DIR = "prototype/src/main/resources/prototypes"

    /**
     * Gets a prototype response for a given prompt.
     *
     * @param prompt The user's input prompt
     * @return A ServerResponse containing the appropriate prototype
     */
    fun getPrototypeForPrompt(prompt: String): PrototypeTemplate {
        val normalizedPrompt = prompt.lowercase()

        /*
        val prototypeDir = File(PROTOTYPES_DIR)

        if (!prototypeDir.exists() || !prototypeDir.isDirectory) {
            println("Warning: Prototypes directory not found at $PROTOTYPES_DIR")
            return PrototypeTemplate(
                chatMessage = "Prototypes directory not found at: $PROTOTYPES_DIR",
                files = JsonObject(emptyMap())
            )
        }
         */

        // val prototypeFiles = prototypeDir.listFiles { file -> file.extension == "json" } ?: emptyArray()

        val fileName = when {
            normalizedPrompt.contains("chatbot") -> "chatbot.json"
            normalizedPrompt.contains("dashboard") -> "dashboard.json"
            normalizedPrompt.contains("tool") -> "tool.json"
            else -> null
        }
        return fileName?.let { val file = this::class.java.getResource(fileName)
            loadPrototype(file)
        } ?:PrototypeTemplate(
            chatMessage = "I didn't find pre-defined prototype for user prompt: $prompt",
            files = JsonObject(emptyMap())
        )

        /*
        // Try to find exact filename match
        for (file in prototypeFiles) {
            val filenameWithoutExtension = file.nameWithoutExtension.lowercase()

            // If the filename is directly contained in the prompt, use this template
            if (normalizedPrompt.contains(filenameWithoutExtension)) {
                return loadPrototype(file)
            }
        }

        // No matching template found, return null to indicate failure
        return PrototypeTemplate(
            chatMessage = "I didn't find pre-defined prototype for user prompt: $prompt",
            files = JsonObject(emptyMap())
        )

         */
    }

    /**
     * Loads a prototype file and creates a response from it.
     *
     * @param file The template file to load
     * @param originalPrompt The original user prompt (for error handling)
     * @return A ServerResponse based on the template file
     */
    private fun loadPrototype(file: URL): PrototypeTemplate {
        return try {
            val content = file.readText()
            val predefinedPrototype = Json.decodeFromString<PredefinedPrototype>(content)

            PrototypeTemplate(
                chatMessage = predefinedPrototype.message,
                files = predefinedPrototype.files
            )
        } catch (e: Exception) {
            println("Error loading hardcoded prototype: ${file}, ${e.message}")
            PrototypeTemplate(
                chatMessage = "There was an error loading pre-defined prototype",
                files = JsonObject(emptyMap())
            )
        }
    }
}