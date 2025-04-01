package prototype

import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse
import prototype.services.LLMServiceFactory

/**
 * Client for the prototype generation workflow that interfaces with language models.
 *
 * @property model The identifier of the language model to use for prompt processing
 */
class PrototypeMain(
    private val route: String = "local",
    private val model: String = "",
) {
    /**
     * Sends a prompt to the language model and returns the generated response.
     *
     * @param prompt The text prompt to send to the language model
     * @param options Options for controlling the behavior of the language model
     * @return The response from the language model, or null if the request failed
     * @throws IllegalStateException If the request to the language model fails
     */
    suspend fun prompt(
        prompt: String,
        options: LLMOptions,
    ): LLMResponse? {
        require(model.isNotBlank()) { "Model name cannot be empty!" }

        val service = LLMServiceFactory.getService(route)
        val result = service.generateResponse(prompt, model, options)

        check(result.isSuccess) { "Failed to receive response from the LLM! Is the model installed?" }

        val response = result.getOrNull()

        // Check that the LLM's response contains the response field, since it could have returned only noise.
        return response
    }
}
