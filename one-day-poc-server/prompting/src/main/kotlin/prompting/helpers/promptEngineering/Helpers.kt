@file:Suppress("ktlint:standard:filename")

package prompting.helpers.promptEngineering

import kotlinx.serialization.Serializable

/**
 * Represents the result of a prompt sanitization operation.
 *
 * This serializable data class contains both the cleaned prompt text and
 * any keywords that were extracted during the sanitization process.
 *
 * @property prompt The sanitized prompt text with potentially sensitive or problematic content removed
 * @property keywords A list of significant terms extracted from the original prompt that can be used for further processing or matching
 */
@Serializable
data class SanitisedPromptResult(
    val prompt: String,
    val keywords: List<String>,
)

/**
 * Represents a generic response containing a timestamp and message.
 *
 * This serializable data class provides a standard format for API responses,
 * particularly for operations that don't need to return complex data structures.
 *
 * @property time A string timestamp indicating when the response was generated
 * @property message The main content of the response, typically providing feedback about the operation result or status information
 */
@Serializable
data class Response(
    val time: String,
    val message: String,
)
