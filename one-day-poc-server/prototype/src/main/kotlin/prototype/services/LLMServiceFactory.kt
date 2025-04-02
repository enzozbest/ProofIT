package prototype.services

/**
 * Factory for creating LLMService instances.
 *
 * This factory provides a way to get the appropriate LLMService implementation
 * based on the route parameter.
 */
object LLMServiceFactory {
    /**
     * Gets the appropriate LLMService implementation for the given route.
     *
     * @param route The route to get the service for (e.g., "local" for Ollama, "openai" for OpenAI)
     * @return The LLMService implementation for the given route
     * @throws IllegalArgumentException If the route is not supported
     */
    fun getService(route: String): LLMService =
        when (route) {
            "local" -> OllamaService
            "openai" -> OpenAIService
            else -> throw IllegalArgumentException("Invalid route $route")
        }
}