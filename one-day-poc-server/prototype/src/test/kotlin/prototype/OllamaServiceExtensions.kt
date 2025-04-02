package prototype

import prototype.helpers.OllamaResponse
import prototype.services.OllamaRequest
import prototype.services.OllamaService

/**
 * Extension function for OllamaService to provide backward compatibility
 * with the old API that took an OllamaRequest object.
 */
suspend fun OllamaService.generateResponse(request: OllamaRequest): Result<OllamaResponse?> {
    return this.generateResponse(
        prompt = request.prompt,
        model = request.model,
        options = request.options
    ) as Result<OllamaResponse?>
}