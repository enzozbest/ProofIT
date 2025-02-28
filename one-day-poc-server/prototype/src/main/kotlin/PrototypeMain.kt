package kcl.seg.rtt.prototype

class PrototypeMain(
    private val model: String,
) {
    suspend fun prompt(prompt: String): LlmResponse? {
        val request = OllamaRequest(prompt, model, false)
        val llmResponse = OllamaService.generateResponse(request)
        check(llmResponse.isSuccess) { "Failed to receive response from the LLM" }
        return llmResponse.getOrNull()
    }
}
