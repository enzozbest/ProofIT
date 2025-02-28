package kcl.seg.rtt.prompting

import helpers.SanitisationTools
import kcl.seg.rtt.prompting.helpers.PromptingTools
import kcl.seg.rtt.prompting.prototypeInteraction.PrototypeInteractor
import kcl.seg.rtt.prototype.OllamaResponse
import kcl.seg.rtt.prototype.PromptException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

data class ChatResponse(
    val response: String,
    val time: String,
)

class PromptingMain(
    private val model: String = "deepseek-coder-v2",
) {
    fun run(userPrompt: String): ChatResponse? {
        val clean = SanitisationTools.sanitisePrompt(userPrompt)
        val freqsPrompt = PromptingTools.functionalRequirementsPrompt(clean.prompt, clean.keywords)

        // First LLM call
        val freqs: OllamaResponse? = promptLlm(freqsPrompt)

        // TODO: Integrate Template retrieval

        val prototypePrompt =
            freqs?.let {
                val freqsJson = Json.decodeFromString<JsonObject>(freqs.response)
                PromptingTools.prototypePrompt(
                    userPrompt,
                    freqsJson["requirements"]?.jsonArray?.joinToString(" ") ?: "",
                    freqsJson["keywords"]?.jsonArray?.joinToString(" ") ?: "",
                )
            } ?: throw PromptException("Failed to generate system prompt")

        // Second LLM call
        val response: OllamaResponse? = promptLlm(prototypePrompt)

        return response?.let {
            println(response)
            val jsonResponse = runCatching { Json.decodeFromString<JsonObject>(response.response) }.getOrNull()
            // Webcontainer.send(webContainerResponse())
            val chatResponse = chatResponse(jsonResponse ?: JsonObject(emptyMap()))
            println(chatResponse)
            chatResponse
        }
    }

    private fun promptLlm(prompt: String): OllamaResponse? =
        runBlocking {
            PrototypeInteractor.prompt(prompt, model)
        }

    private fun chatResponse(jsonResponse: JsonObject): ChatResponse =
        ChatResponse(
            response = "Here is your prototype: ${jsonResponse.jsonPrimitive.content}",
            time = Instant.now().toString(),
        )

    private fun webContainerResponse() {
        // TODO extract web container response from model response
    }
}
