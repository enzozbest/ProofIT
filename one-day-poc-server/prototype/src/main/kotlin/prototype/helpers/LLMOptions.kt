package prototype.helpers

import kotlinx.serialization.Serializable

sealed class LLMOptions

@Serializable
data class OpenAIOptions(
    val temperature: Double = 0.40,
    val top_p: Double = 0.95,
    val max_tokens: Int = 1000,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.0,
    val stop: List<String> = emptyList(),
) : LLMOptions()

/**
 * Configuration options for Ollama model generation.
 * These parameters control the behavior of the language model during text generation.
 *
 * @property temperature Controls randomness in generation. Higher values (e.g., 0.8) make output more random,
 *                      lower values (e.g., 0.2) make it more deterministic. Range: 0.0-1.0
 * @property top_k Limits token selection to the top K most likely tokens. Higher values allow more diversity.
 * @property top_p Nucleus sampling - consider tokens comprising the top_p probability mass. Range: 0.0-1.0
 * @property num_predict Maximum number of tokens to generate
 * @property stop List of strings that will stop generation when encountered
 */
@Serializable
data class OllamaOptions(
    val temperature: Double? = null,
    val top_k: Int? = null,
    val top_p: Double? = null,
    val num_predict: Int? = null,
    val stop: List<String>? = null,
) : LLMOptions()
