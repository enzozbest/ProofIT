package kcl.seg.rtt.prompting

import helpers.SanitisationTools
import kcl.seg.rtt.prompting.helpers.PromptingTools
import kcl.seg.rtt.prompting.helpers.PrototypeInteractor
import kcl.seg.rtt.prototype.LlmResponse
import kcl.seg.rtt.prototype.PromptException
import kcl.seg.rtt.prototype.secureCodeCheck
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

data class ChatResponse(
    val response: String,
    val time: String,
)

class PromptingMain(
    private val model: String = "deepseek-r1:32b",
) {
    fun run(userPrompt: String): ChatResponse {
        val sanitisedPrompt = SanitisationTools.sanitisePrompt(userPrompt)
        val freqsPrompt = PromptingTools.functionalRequirementsPrompt(sanitisedPrompt.prompt, sanitisedPrompt.keywords)

        // First LLM call
        val freqs: JsonObject = promptLlm(freqsPrompt)

        // TODO: Integrate Template retrieval
        val prototypePrompt = prototypePrompt(userPrompt, freqs)

        // Second LLM call
        val response: JsonObject = promptLlm(prototypePrompt)
        println(response)
        return chatResponse(response)
    }

    private fun prototypePrompt(
        userPrompt: String,
        freqsResponse: JsonObject,
    ): String {
        val reqs =
            runCatching {
                (freqsResponse["requirements"] as JsonArray).joinToString(" ")
            }.getOrElse {
                throw PromptException("Failed to extract requirements from LLM response")
            }
        val keywords =
            runCatching {
                (freqsResponse["keywords"] as JsonArray).joinToString(" ")
            }.getOrElse {
                throw PromptException("Failed to extract keywords from LLM response")
            }
        return PromptingTools.prototypePrompt(
            userPrompt,
            reqs,
            keywords,
        )
    }

    private fun promptLlm(prompt: String): JsonObject =
        runBlocking {
            val llmResponse = PrototypeInteractor.prompt(prompt, model) ?: throw PromptException("LLM did not respond!")
            println(llmResponse.response)
            PromptingTools.formatResponseJson(llmResponse.response)
        }

    /**
     * Extracts the functional requirements from the LLM response and returns them as a ChatResponse.
     * @param response The LLM response.
     * @return A ChatResponse containing the functional requirements.
     * @throws PromptException If the requirements could not be found or were returned in an unrecognised format.
     */
    private fun chatResponse(response: JsonObject): ChatResponse {
        val reqs =
            when (val jsonReqs = response["requirements"]) {
                is JsonArray -> {
                    if (jsonReqs.isEmpty()) {
                        throw PromptException("No requirements found in LLM response")
                    }
                    jsonReqs.map { (it as JsonPrimitive).content }.joinToString(", ")
                }

                is JsonPrimitive -> jsonReqs.content
                else -> throw PromptException("Requirements could not be found or were returned in an unrecognised format.")
            }

        return ChatResponse(
            response = "These are the functional requirements fulfilled by this prototype: $reqs",
            time = Instant.now().toString(),
        )
    }

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
