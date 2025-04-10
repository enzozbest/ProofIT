package prompting.helpers

import prototype.helpers.LLMOptions
import prototype.helpers.OllamaOptions
import prototype.helpers.OpenAIOptions

/**
 * Factory for creating LLMOptions instances.
 *
 * This factory provides a way to get the appropriate LLMOptions implementation
 * based on the route parameter.
 */
object OptionsFactory {
    /**
     * Gets the appropriate LLMOptions implementation for the given route.
     *
     * @param route The route to get the options for (e.g., "local" for Ollama, "openai" for OpenAI)
     * @param temperature The temperature parameter for controlling randomness
     * @return The LLMOptions implementation for the given route
     * @throws IllegalArgumentException If the route is not supported
     */
    fun getOptions(
        route: String,
        temperature: Double,
    ): LLMOptions =
        when (route) {
            "local" -> OllamaOptions(temperature = temperature, topK = 300, topP = 0.9)
            "openai" -> OpenAIOptions(temperature = temperature)
            else -> throw IllegalArgumentException("Invalid route $route")
        }
}
