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
            OllamaRequest(prompt = prompt, model = model, stream = false, options = options).also { println(it) }
        val llmResponse = OllamaService.generateResponse(request)
        check(llmResponse.isSuccess) { "Failed to receive response from the LLM" }
        return llmResponse.getOrNull()
    }
}
