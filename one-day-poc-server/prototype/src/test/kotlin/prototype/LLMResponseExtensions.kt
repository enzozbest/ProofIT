package prototype

import prototype.helpers.LLMResponse
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIResponse

/**
 * Extension property for LLMResponse to provide backward compatibility
 * with the old API that accessed the response property directly.
 */
val LLMResponse.response: String?
    get() = when (this) {
        is OllamaResponse -> this.response
        is OpenAIResponse -> this.response
    }