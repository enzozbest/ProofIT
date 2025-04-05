package prompting.helpers

import prototype.helpers.LLMResponse
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIResponse
import prompting.helpers.promptEngineering.PromptingTools

/**
 * Interface for formatting responses from language models.
 *
 * This interface defines the contract for classes that format responses
 * from language models into a standardized format.
 */
interface ResponseFormatter {
    /**
     * Formats a response from a language model.
     *
     * @param response The response from the language model
     * @return The formatted response as a string
     */
    fun format(response: LLMResponse): String
}

/**
 * Formatter for Ollama responses.
 */
class OllamaResponseFormatter : ResponseFormatter {
    override fun format(response: LLMResponse): String {
        if (response !is OllamaResponse) {
            throw IllegalArgumentException("Expected OllamaResponse but got ${response::class.simpleName}")
        }
        
        return PromptingTools.formatResponseJson(
            response.response ?: throw RuntimeException("LLM response was null!")
        )
    }
}

/**
 * Formatter for OpenAI responses.
 */
class OpenAIResponseFormatter : ResponseFormatter {
    override fun format(response: LLMResponse): String {
        if (response !is OpenAIResponse) {
            throw IllegalArgumentException("Expected OpenAIResponse but got ${response::class.simpleName}")
        }
        
        return PromptingTools.formatResponseJson(
            response.response ?: throw RuntimeException("LLM response was null!")
        )
    }
}

/**
 * Factory for creating ResponseFormatter instances.
 */
object ResponseFormatterFactory {
    /**
     * Gets the appropriate ResponseFormatter for the given route.
     *
     * @param route The route to get the formatter for (e.g., "local" for Ollama, "openai" for OpenAI)
     * @return The ResponseFormatter for the given route
     * @throws IllegalArgumentException If the route is not supported
     */
    fun getFormatter(route: String): ResponseFormatter =
        when (route) {
            "local" -> OllamaResponseFormatter()
            "openai" -> OpenAIResponseFormatter()
            else -> throw IllegalArgumentException("Invalid route $route")
        }
}