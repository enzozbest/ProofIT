package prompting.helpers

import prompting.helpers.promptEngineering.PromptingTools

/**
 * Interface for formatting prompts for language models.
 *
 * This interface defines the contract for classes that format prompts
 * for language models into a standardized format.
 */
interface PromptFormatter {
    /**
     * Formats a prompt for a language model.
     *
     * @param userPrompt The original user prompt
     * @param requirements The extracted requirements
     * @param templates The templates to include
     * @param previousGeneration The previous generation, if any
     * @return The formatted prompt as a string
     */
    fun format(
        userPrompt: String,
        requirements: String,
        templates: List<String>,
        previousGeneration: String?
    ): String
}

/**
 * Formatter for Ollama prompts.
 */
class OllamaPromptFormatter : PromptFormatter {
    override fun format(
        userPrompt: String,
        requirements: String,
        templates: List<String>,
        previousGeneration: String?
    ): String {
        return PromptingTools.ollamaPrompt(
            userPrompt,
            requirements,
            templates,
            previousGeneration
        )
    }
}

/**
 * Formatter for OpenAI prompts.
 */
class OpenAIPromptFormatter : PromptFormatter {
    override fun format(
        userPrompt: String,
        requirements: String,
        templates: List<String>,
        previousGeneration: String?
    ): String {
        return PromptingTools.openAIPrompt(
            userPrompt,
            requirements,
            templates,
            previousGeneration
        )
    }
}

/**
 * Factory for creating PromptFormatter instances.
 */
object PromptFormatterFactory {
    /**
     * Gets the appropriate PromptFormatter for the given route.
     *
     * @param route The route to get the formatter for (e.g., "local" for Ollama, "openai" for OpenAI)
     * @return The PromptFormatter for the given route
     * @throws IllegalArgumentException If the route is not supported
     */
    fun getFormatter(route: String): PromptFormatter =
        when (route) {
            "local" -> OllamaPromptFormatter()
            "openai" -> OpenAIPromptFormatter()
            else -> throw IllegalArgumentException("Invalid route $route")
        }
}