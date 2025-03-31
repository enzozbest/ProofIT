package prompting

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import prompting.helpers.PrototypeInteractor
import prompting.helpers.promptEngineering.PromptingTools
import prompting.helpers.promptEngineering.SanitisationTools
import prompting.helpers.templates.TemplateInteractor
import prototype.LlmResponse
import prototype.helpers.LLMOptions
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse
import prototype.helpers.PromptException
import prototype.security.secureCodeCheck
import utils.environment.EnvironmentLoader

/**
 * Represents a response from the chat processing system.
 *
 * @property message The generated text response from the LLM
 * @property role The role of the responder (default: "LLM")
 * @property timestamp Timestamp string indicating when the response was created
 */
@Serializable
data class ChatResponse(
    val message: String,
    val role: String = "LLM",
    val timestamp: String,
    val messageId: String,
)

@Serializable
data class PrototypeResponse(
    val files: String, // Keep as JsonObject, not Map
)

/**
 * Main orchestrator for the multi-step prompting workflow.
 *
 * This class manages the entire process flow for generating responses from
 * user prompts, including prompt sanitization, requirements extraction, template
 * fetching, and final prototype generation.
 *
 * @property ollamaModel The LLM model identifier to use for prompt processing (default: "qwen2.5-coder:14b")
 */
class PromptingMain(
    private val ollamaModel: String = EnvironmentLoader.get("OLLAMA_MODEL"),
    private val openAIModel: String = EnvironmentLoader.get("OPENAI_MODEL"),
) {
    /**
     * Executes the complete prompting workflow for a user prompt.
     *
     * This method processes the user's prompt through multiple steps:
     * 1. Sanitizes the input prompt to ensure safety
     * 2. Generates a specialised prompt to extract functional requirements
     * 3. Makes first LLM call to get requirements analysis
     * 4. Creates a prompt for template retrieval and fetches matching templates
     * 5. Creates a comprehensive prototype prompt with requirements and templates
     * 6. Makes second LLM call to generate the final prototype response
     * 7. Validates the response returned by the LLM
     * 8. Formats and returns the final chat response
     *
     * @param userPrompt The raw text prompt from the user
     * @return A ServerResponse object containing the generated response and timestamp
     * @throws PromptException If any step in the prompting workflow fails
     */
    suspend fun run(
        userPrompt: String,
        previousGeneration: String? = null,
    ): String {
        // Step 1: Sanitise the user prompt
        val sanitisedPrompt = SanitisationTools.sanitisePrompt(userPrompt)

        // Step 2: Extract functional requirements
        val freqsPrompt = PromptingTools.functionalRequirementsPrompt(sanitisedPrompt.prompt, sanitisedPrompt.keywords)
        val freqsOptions = OllamaOptions(temperature = 0.50, top_k = 300, top_p = 0.9, num_predict = 500)
        val freqs: String = promptLlm(freqsPrompt, freqsOptions, "local")
        val freqsResponse: JsonObject =
            runCatching { Json.decodeFromString<JsonObject>(freqs) }.getOrElse { buildJsonObject { } }
        val requirements = freqsResponse["requirements"]?.jsonArray?.joinToString(",") ?: ""

        // Step 3: Fetch templates based on requirements and user prompt
        val fetcherInput = "$requirements, $userPrompt"
        val templates = TemplateInteractor.fetchTemplates(fetcherInput)

        // Step 4: Generate the prototype
        val route =
            when (EnvironmentLoader.get("USE_OPENAI").toBoolean()) {
                true -> "openai"
                false -> "local"
            }

        val prototypePrompt =
            prototypePrompt(userPrompt, freqsResponse, templates, previousGeneration, route).also { println(it) }
        val prototypeOptions =
            when (route) {
                "local" -> OllamaOptions(temperature = 0.40, top_k = 300, top_p = 0.9)
                "openai" -> OpenAIOptions(temperature = 0.40)
                else -> throw IllegalArgumentException("Invalid route $route")
            }.also { println(it) }

        val prototypeResponse: String = promptLlm(prototypePrompt, prototypeOptions, route)
        return prototypeResponse.also { println(it) }
    }

    /**
     * Creates a specialised prompt for generating a prototype based on requirements and templates.
     *
     * This method formats a prompt that includes:
     * - The original user prompt
     * - Extracted functional requirements
     * - Optional templates for suggested components (if available)
     *
     * @param userPrompt The original text prompt from the user
     * @param freqsResponse The JSON object containing extracted requirements data
     * @param templates List of component templates to include (empty by default)
     * @return A formatted prompt string ready to be sent to the LLM
     * @throws PromptException If requirements or keywords cannot be extracted from freqsResponse
     */
    private fun prototypePrompt(
        userPrompt: String,
        freqsResponse: JsonObject,
        templates: List<String> = emptyList(),
        previousGeneration: String? = null,
        route: String,
    ): String {
        val reqs =
            runCatching {
                (freqsResponse["requirements"] as JsonArray).joinToString(" ")
            }.getOrElse {
                throw PromptException("Failed to extract requirements from LLM response")
            }

        if (!freqsResponse.containsKey("keywords")) {
            throw PromptException("Failed to extract keywords from LLM response")
        }

        runCatching {
            (freqsResponse["keywords"] as JsonArray).map { (it as JsonPrimitive).content }
        }.getOrDefault(emptyList())

        return if (route == "local") {
            PromptingTools.ollamaPrompt(
                userPrompt,
                reqs,
                templates,
                previousGeneration,
            )
        } else {
            PromptingTools.openAIPrompt(
                userPrompt,
                reqs,
                templates,
                previousGeneration,
            )
        }
    }

    /**
     * Sends a prompt to the LLM and processes the response into a JsonObject.
     *
     * @param prompt The formatted prompt text to send to the LLM
     * @return A JsonObject containing the parsed response from the LLM
     * @throws PromptException If the LLM does not respond or if the response cannot be parsed
     */
    private fun promptLlm(
        prompt: String,
        options: LLMOptions,
        route: String,
    ): String =
        runBlocking {
            val model =
                when (route) {
                    "local" -> ollamaModel
                    "openai" -> openAIModel
                    else -> throw IllegalArgumentException("Invalid route $route")
                }

            val llmResponse =
                PrototypeInteractor.prompt(prompt, model, route, options)
                    ?: throw PromptException("LLM did not respond!")
            if (route == "local") {
                PromptingTools.formatResponseJson(
                    (llmResponse as OllamaResponse).response ?: throw PromptException("LLM response was null!"),
                )
            } else {
                PromptingTools.formatResponseJson(
                    (llmResponse as OpenAIResponse).response ?: throw PromptException("LLM response was null!"),
                )
            }
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
