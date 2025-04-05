package prototype.services

import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse

/**
 * Interface for language model services.
 *
 * This interface defines the contract for services that interact with language models.
 * Implementations of this interface can connect to different language model providers,
 * such as Ollama or OpenAI.
 */
interface LLMService {
    /**
     * Sends a prompt to the language model and returns the generated response.
     *
     * @param prompt The text prompt to send to the language model
     * @param model The identifier of the language model to use
     * @param options Options for controlling the behavior of the language model
     * @return The response from the language model, or null if the request failed
     */
    suspend fun generateResponse(
        prompt: String,
        model: String,
        options: LLMOptions
    ): Result<LLMResponse?>
}