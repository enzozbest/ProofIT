package prototype.helpers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class LLMOptions

@Serializable
data class OpenAIOptions(
    val temperature: Double = 0.40,
    @SerialName("top_p") val topP: Double = 0.95,
) : LLMOptions()

/**
 * Configuration options for Ollama model generation.
 * These parameters control the behavior of the language model during text generation.
 *
 * @property temperature Controls randomness in generation. Higher values (e.g., 0.8) make output more random,
 *                      lower values (e.g., 0.2) make it more deterministic. Range: 0.0-1.0
 * @property topK Limits token selection to the top K most likely tokens. Higher values allow more diversity.
 * @property topP Nucleus sampling - consider tokens comprising the top_p probability mass. Range: 0.0-1.0
 * @property numPredict Maximum number of tokens to generate
 * @property stop List of strings that will stop generation when encountered
 */
@Serializable
data class OllamaOptions(
    val temperature: Double? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("num_predict") val numPredict: Int? = null,
    val stop: List<String>? = null,
) : LLMOptions()
