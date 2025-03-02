package kcl.seg.rtt.prompting

import helpers.SanitisationTools
import io.ktor.server.routing.*
import kcl.seg.rtt.prompting.helpers.PromptingTools
import kcl.seg.rtt.prompting.prototypeInteraction.PrototypeInteractor
import kcl.seg.rtt.prototype.FileContent
import kcl.seg.rtt.prototype.LlmResponse
import kcl.seg.rtt.prototype.OllamaResponse
import kcl.seg.rtt.prototype.PromptException
import kcl.seg.rtt.prototype.secureCodeCheck
import kcl.seg.rtt.webcontainer.WebContainerState
import kcl.seg.rtt.webcontainer.parseCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
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
            val languageMap: Map<String, String> = jsonResponse?.mapValues {
                val codeElement = it.value
                // if it's just a primitive string, you can do:
                codeElement.jsonPrimitive.contentOrNull ?: ""
            } ?: emptyMap()
            val llmResponse = LlmResponse(
                // Possibly discard mainFile or set it to "N/A"
                mainFile = response.response,
                files = languageMap.mapValues { (_, snippet) -> FileContent(snippet) }
            )
            onSiteSecurityCheck(llmResponse)
            WebContainerState.updateResponse(llmResponse)
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

    private fun onSiteSecurityCheck(llmResponse: LlmResponse) {
        for ((language, fileContent) in llmResponse.files) {
            val codeSnippet = fileContent.content
            if (!secureCodeCheck(codeSnippet, language)) {
                throw RuntimeException("Code is not safe for language=$language")
            }
        }
    }

    private fun webContainerResponse() {
        // TODO extract web container response from model response
    }
}
