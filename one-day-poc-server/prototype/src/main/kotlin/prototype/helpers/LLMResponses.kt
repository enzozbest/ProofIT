package prototype.helpers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

sealed class LLMResponse

/**
 * Contains the complete response data returned by the Ollama service.
 *
 * @property model The identifier of the model that generated the response
 * @property created_at ISO 8601 timestamp when the response was generated
 * @property response The actual text generated by the language model
 * @property done Boolean indicating if the generation is complete
 * @property done_reason Reason code for completion (e.g., "stop", "length", "error")
 */
@Serializable
data class OllamaResponse(
    val model: String,
    val created_at: String,
    val response: String?,
    val done: Boolean,
    val done_reason: String,
) : LLMResponse()

/**
 * Contains the simplified response data returned by the OpenAI service.
 *
 * @property model The identifier of the model that generated the response
 * @property createdAt ISO 8601 timestamp when the response was generated
 * @property response The actual text generated by the language model
 * @property done Boolean indicating if the generation is complete
 * @property doneReason Reason code for completion (e.g., "stop", "length", "error")
 */
@Serializable
data class OpenAIResponse(
    val model: String,
    @SerialName("created_at") val createdAt: Long,
    val response: String?,
    val done: Boolean = true,
    @SerialName("done_reason") val doneReason: String = "stop",
) : LLMResponse()

@Serializable
internal data class OpenAIApiResponse(
    val id: String,
    val `object`: String,
    @SerialName("created_at") val createdAt: Long,
    val status: String,
    val error: String? = null,
    @SerialName("incomplete_details") val incompleteDetails: String? = null,
    val instructions: String? = null,
    @SerialName("max_output_tokens") val maxOutputTokens: Int? = null,
    val model: String,
    val output: List<OutputItem> = emptyList(),
    @SerialName("parallel_tool_calls") val parallelToolCalls: Boolean = false,
    @SerialName("previous_response_id") val previousResponseId: String? = null,
    val reasoning: Reasoning? = null,
    val store: Boolean = true,
    val temperature: Double? = null,
    val text: TextFormat? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
    val tools: List<JsonElement> = emptyList(),
    @SerialName("top_p") val topP: Double? = null,
    val truncation: String? = null,
    val usage: Usage? = null,
    val user: String? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class OutputItem(
    val type: String,
    val id: String,
    val status: String? = null,
    val role: String? = null,
    val content: List<ContentItem> = emptyList(),
)

@Serializable
data class ContentItem(
    val type: String,
    val text: String,
    val annotations: List<JsonElement> = emptyList(),
)

@Serializable
data class Reasoning(
    val effort: String? = null,
    @SerialName("generate_summary") val generateSummary: String? = null,
)

@Serializable
data class TextFormat(
    val format: Format? = null,
)

@Serializable
data class Format(
    val type: String? = null,
)

@Serializable
data class Usage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("input_tokens_details") val inputTokensDetails: InputTokensDetails? = null,
    @SerialName("output_tokens") val outputTokens: Int,
    @SerialName("output_tokens_details") val outputTokensDetails: OutputTokensDetails? = null,
    @SerialName("total_tokens") val totalTokens: Int,
)

@Serializable
data class InputTokensDetails(
    @SerialName("cached_tokens") val cachedTokens: Int? = null,
)

@Serializable
data class OutputTokensDetails(
    @SerialName("reasoning_tokens") val reasoningTokens: Int? = null,
)

/**
 * Parses the OpenAI API response from a JSON string into an OpenAIResponse object.
 *
 * @param jsonString The JSON string to parse
 * @return An [OpenAIResponse] object containing the parsed data
 */
fun parseOpenAIResponse(jsonString: String): OpenAIResponse {
    val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    println("DECODING FULL STRING")
    val fullResponse = json.decodeFromString<OpenAIApiResponse>(jsonString)
    println("DECODED FULL STRING")
    return fullResponse.toOpenAIResponse()
}

/**
 * Converts the full API response to a simplified OpenAIResponse
 */
private fun OpenAIApiResponse.toOpenAIResponse(): OpenAIResponse {
    println("EXTRACTING CONTENT")
    val textContent =
        output
            .asSequence()
            .mapNotNull { it.content.firstOrNull()?.text }
            .firstOrNull { it.isNotBlank() } ?: ""

    println("BUILDING RESPONSE")
    return OpenAIResponse(
        model = model,
        createdAt = createdAt,
        response = textContent,
    )
}
