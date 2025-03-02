package kcl.seg.rtt.prototype

class PrototypeMain(
    private val model: String,
) {
    suspend fun prompt(prompt: String): OllamaResponse? {
        val request = OllamaRequest(prompt = prompt, model = model, stream = false)
        val llmResponse = OllamaService.generateResponse(request)
        check(llmResponse.isSuccess) { "Failed to receive response from the LLM" }

        for ((language, fileContent) in llmResponse.files) {
            // Extract the actual snippet string
            val codeSnippet = fileContent.content

            // 2) Security checks
            val isSafe = secureCodeCheck(codeSnippet, language)
            if (!isSafe) {
                return Result.failure(
                    RuntimeException("Code is not safe for language=$language")
                )
            }
        return llmResponse.getOrNull()
    }
}
