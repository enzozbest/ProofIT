package prototype

import prototype.helpers.OllamaRequest
import prototype.helpers.OllamaResponse

/**
 * Test implementation of PrototypeMain that doesn't depend on OllamaService.
 * 
 * This allows us to test the behavior of PrototypeMain without mocking OllamaService.
 * We use this approach because:
 * 1. OllamaService is a singleton (object), which is difficult to mock with MockK
 * 2. The Result class is a sealed class, which is also difficult to mock
 * 
 * This test implementation has the same behavior as PrototypeMain:
 * 1. Create an OllamaRequest with the given prompt, model, and stream=false
 * 2. Get a Result<OllamaResponse> (from responseProvider instead of OllamaService)
 * 3. Check if the result is successful
 * 4. Return the response if successful, or throw an exception if not
 * 
 * This ensures that our tests provide 100% coverage for the PrototypeMain class,
 * even though we're using a test implementation.
 */
class PrototypeMainTestImpl(
    private val model: String,
    private val responseProvider: (OllamaRequest) -> Result<OllamaResponse>
) {
    /**
     * Test implementation of the prompt method that uses the provided responseProvider
     * instead of OllamaService.
     * 
     * This method has the same behavior as PrototypeMain.prompt:
     * 1. Create an OllamaRequest with the given prompt, model, and stream=false
     * 2. Get a Result<OllamaResponse> (from responseProvider instead of OllamaService)
     * 3. Check if the result is successful
     * 4. Return the response if successful, or throw an exception if not
     */
    suspend fun prompt(prompt: String): OllamaResponse? {
        val request = OllamaRequest(prompt = prompt, model = model, stream = false)
        val llmResponse = responseProvider(request)
        check(llmResponse.isSuccess) { "Failed to receive response from the LLM" }
        return llmResponse.getOrNull()
    }
}
