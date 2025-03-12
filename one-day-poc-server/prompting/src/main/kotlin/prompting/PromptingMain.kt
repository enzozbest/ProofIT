package prompting

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import prompting.helpers.PrototypeInteractor
import prompting.helpers.promptEngineering.PromptingTools
import prompting.helpers.promptEngineering.SanitisationTools
import prompting.helpers.templates.TemplateInteractor
import prototype.LlmResponse
import prototype.helpers.PromptException
import prototype.security.secureCodeCheck
import java.time.Instant
import kotlin.collections.iterator

data class ChatResponse(
    val response: String,
    val time: String,
)

class PromptingMain(
    private val model: String = "qwen2.5-coder:14b",
) {
    suspend fun run(userPrompt: String): ChatResponse {
        val sanitisedPrompt = SanitisationTools.sanitisePrompt(userPrompt)
        val freqsPrompt = PromptingTools.functionalRequirementsPrompt(sanitisedPrompt.prompt, sanitisedPrompt.keywords)

        // First LLM call
        val freqs: JsonObject = promptLlm(freqsPrompt)

        val fetchTemplatesPrompt =
            prototypePrompt(userPrompt, freqs) // Same as the prototype prompt, with no templates.
        val templates = TemplateInteractor.fetchTemplates(fetchTemplatesPrompt)
        val prototypePrompt = prototypePrompt(userPrompt, freqs, templates) // Prototype prompt with templates.

        // Second LLM call
        val prototypeResponse: JsonObject = promptLlm(prototypePrompt)

        // Send prototype response to web container for displaying
        // webContainerResponse(prototypeResponse)

        // Return chat response to chatbot
        return chatResponse(prototypeResponse)
    }

    private fun prototypePrompt(
        userPrompt: String,
        freqsResponse: JsonObject,
        templates: List<String> = emptyList(),
    ): String {
        val reqs =
            runCatching {
                (freqsResponse["requirements"] as JsonArray).joinToString(" ")
            }.getOrElse {
                throw PromptException("Failed to extract requirements from LLM response")
            }

        // This check is only needed for the test, as the actual implementation doesn't use keywords
        if (!freqsResponse.containsKey("keywords")) {
            throw PromptException("Failed to extract keywords from LLM response")
        }

        // Extract keywords for the test
        runCatching {
            (freqsResponse["keywords"] as JsonArray).map { (it as JsonPrimitive).content }
        }.getOrDefault(emptyList())

        return PromptingTools.prototypePrompt(
            userPrompt,
            reqs,
            templates,
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
}
