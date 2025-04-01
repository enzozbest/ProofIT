package prompting.helpers

import prototype.PrototypeMain
import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse

/**
 * Helper class for interacting with the prototype module.
 *
 * This class provides a simple interface for sending prompts to language models
 * via the prototype module.
 */
object PrototypeInteractor {
    /**
     * Sends a prompt to a language model and returns the generated response.
     *
     * @param prompt The text prompt to send to the language model
     * @param model The identifier of the language model to use
     * @param route The route to use for the language model (e.g., "local" for Ollama, "openai" for OpenAI)
     * @param options Options for controlling the behavior of the language model
     * @return The response from the language model, or null if the request failed
     */
    suspend fun prompt(
        prompt: String,
        model: String,
        route: String,
        options: LLMOptions,
    ): LLMResponse? {
        val prototypeMain = PrototypeMain(route = route, model = model)
        return prototypeMain.prompt(prompt, options)
    }
}
