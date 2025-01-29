package kcl.seg.rtt.prototype

class PrototypeService {
    fun generatePrototype(prompt: String, context: List<String>?): String {
        val fullPrompt = buildFullPrompt(prompt, context)
        val llmResponse = callLLM(fullPrompt)
        return llmResponse
    }

    private fun buildFullPrompt(prompt: String, context: List<String>?): String {
        return if (context != null) {
            context.joinToString("\n") + "\n" + prompt
        } else {
            prompt
        }
    }

    private fun callLLM(prompt: String): String {
        // Interact with LLM
        return "Generated prototype based on prompt: $prompt"
    }
}