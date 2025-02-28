package kcl.seg.rtt.prototype

class PrototypeMain(
    private val model: String,
) {
    suspend fun prompt(prompt: String): OllamaResponse? {
        val request = OllamaRequest(prompt = prompt, model = model, stream = false)
        val llmResponse = OllamaService.generateResponse(request)
        check(llmResponse.isSuccess) { "Failed to receive response from the LLM" }
        return llmResponse.getOrNull()
    }
}
