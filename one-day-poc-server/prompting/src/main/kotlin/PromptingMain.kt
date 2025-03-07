package kcl.seg.rtt.prompting

import helpers.SanitisationTools
import kcl.seg.rtt.prompting.helpers.PromptingTools
import kcl.seg.rtt.prompting.helpers.PrototypeInteractor
import kcl.seg.rtt.prototype.LlmResponse
import kcl.seg.rtt.prototype.PromptException
import kcl.seg.rtt.prototype.convertJsonToLlmResponse
import kcl.seg.rtt.prototype.secureCodeCheck
import kcl.seg.rtt.webcontainer.WebContainerState
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
        val prototypeResponse: JsonObject = promptLlm(prototypePrompt)

        // Send prototype response to web container for displaying
        // TODO: webContainerResponse(prototypeResponse)

        // Return chat response to chatbot
        return chatResponse(prototypeResponse)
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

    /**
     * Prompts the LLM with the given prompt and returns the response as a JsonObject.
     */
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
                    jsonReqs.joinToString(", ") { (it as JsonPrimitive).content }
                }

                is JsonPrimitive -> jsonReqs.content
                else -> throw PromptException("Requirements could not be found or were returned in an unrecognised format.")
            }

        return ChatResponse(
            response = "These are the functional requirements fulfilled by this prototype: $reqs",
            time = Instant.now().toString(),
        )
    }

    /**
     * Checks the security of the code snippets in the LLM response.
     * @param llmResponse The LLM response.
     * @throws RuntimeException If the code is not safe for any of the languages used.
     */
    private fun onSiteSecurityCheck(llmResponse: LlmResponse) {
        for ((language, fileContent) in llmResponse.files) {
            val codeSnippet = fileContent.content
            if (!secureCodeCheck(codeSnippet, language)) {
                throw RuntimeException("Code is not safe for language=$language")
            }
        }
    }

    /**
     * Sends the prototype response to the web container for display.
     */
    private fun webContainerResponse(prototypeResponse: JsonObject) {
        val llmResponseObject = convertJsonToLlmResponse(prototypeResponse)
        onSiteSecurityCheck(llmResponseObject)
        WebContainerState.updateResponse(llmResponseObject)
    }
}
