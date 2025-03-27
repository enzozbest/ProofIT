package prototype

import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaRequest
import prototype.helpers.OllamaResponse
import prototype.helpers.OllamaService

/**
 * Client for the prototype generation workflow that interfaces with language models.
 *
 * @property model The identifier of the language model to use for prompt processing
 */
class PrototypeMain(
    private val model: String,
) {
    /**
     * Sends a prompt to the language model and returns the generated response.
     *
     * @param prompt The text prompt to send to the language model
     * @return The response from the language model, or null if the request failed
     * @throws IllegalStateException If the request to the language model fails
     */
    suspend fun prompt(
        prompt: String,
        options: OllamaOptions,
    ): OllamaResponse? {
        val request =
            OllamaRequest(prompt = prompt, model = model, stream = false, options = options)
        val llmResponse = OllamaService.generateResponse(request)
        if (!llmResponse.isSuccess) {
            throw IllegalStateException("Failed to receive response from the LLM")
        }
        val response = llmResponse.getOrNull()
        // Return null if the response itself is null or if the response field is null
        return if (response?.response == null) null else response
    }
}
