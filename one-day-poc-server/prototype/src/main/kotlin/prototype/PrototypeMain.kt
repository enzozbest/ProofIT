package prototype

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse
import prototype.services.OllamaRequest
import prototype.services.OllamaService
import prototype.services.OpenAIService
import utils.environment.EnvironmentLoader

/**
 * Client for the prototype generation workflow that interfaces with language models.
 *
 * @property model The identifier of the language model to use for prompt processing
 */
class PrototypeMain(
    private val route: String = "local",
    private val model: String = "",
) {
    /**
     * Sends a prompt to the language model and returns the generated response.
     *
     * @param prompt The text prompt to send to the language model
     * @return The response from the language model, or null if the request failed
     * @throws IllegalStateException If the request to the language model fails
     */
    suspend fun prompt(
        prompt: String,
        options: LLMOptions,
    ): LLMResponse? {
        require(model.isNotBlank()) { "Model name cannot be empty!" }
        return when (route) {
            "local" -> promptOllama(prompt, options as OllamaOptions)
            "openai" -> promptOpenAI(prompt, options as OpenAIOptions)
            else -> throw IllegalArgumentException("Invalid route $route")
        }
    }

    private suspend fun promptOllama(
        prompt: String,
        options: OllamaOptions,
    ): OllamaResponse? {
        val request =
            OllamaRequest(prompt = prompt, model = model, stream = false, options = options)
        val llmResponse = OllamaService.generateResponse(request)

        check(llmResponse.isSuccess) { "Failed to receive response from the LLM! Is the model installed?" }

        val response = llmResponse.getOrNull() as OllamaResponse?

        // Check that the LLM's response contains the response field, since it could have returned only noise.
        return if (response?.response == null) null else response
    }

    private suspend fun promptOpenAI(
        prompt: String,
        options: OpenAIOptions,
    ): OpenAIResponse? {
        val request =
            buildOpenAIRequest(
                apiHost = EnvironmentLoader.get("OPENAI_HOST"),
                apiPath = EnvironmentLoader.get("OPENAI_PATH"),
                apiKey = EnvironmentLoader.get("OPENAI_API_KEY"),
                organisationId = EnvironmentLoader.get("OPENAI_ORGANISATION_ID"),
                instructions = generateInstructions(),
                prompt = prompt,
                projectId = EnvironmentLoader.get("OPENAI_PROJECT_ID"),
            )

        return OpenAIService.callOpenAI(request, options)
    }

    private fun buildOpenAIRequest(
        apiHost: String,
        apiPath: String,
        apiKey: String,
        organisationId: String,
        projectId: String,
        instructions: String,
        prompt: String,
    ): HttpRequestBuilder =
        HttpRequestBuilder()
            .apply {
                method = io.ktor.http.HttpMethod.Post
                url {
                    protocol = URLProtocol.HTTP
                    host = apiHost
                    path(apiPath)
                }
                header("Authorization", "Bearer $apiKey")
                if (organisationId.isNotBlank()) {
                    header("OpenAI-Organization", organisationId)
                }
                if (projectId.isNotBlank()) {
                    header("OpenAI-Project", projectId)
                }
                header("Content-Type", "application/json")
                val jsonRequestObject =
                    buildJsonObject {
                        put("model", JsonPrimitive(model))
                        put("input", JsonPrimitive(prompt))
                        if (instructions.isNotBlank()) {
                            setBody("instructions" to instructions)
                        }
                    }
                setBody(Json.encodeToString(JsonObject.serializer(), jsonRequestObject))
            }.also { println(it) }

    private fun generateInstructions() =
        """
        "format": {
            "type": "json_schema",
            "name": "prototype_code",
            "schema": {
              "${'$'}schema": "http://json-schema.org/draft-07/schema#",
              "title": "Ollama Response Schema",
              "type": "object",
              "properties": {
                "chat": {
                  "type": "string",
                  "description": "Chat response from the server",
                },
                "prototype": {
                  "type": "object",
                  "description": "Prototype files to render in WebContainer",
                  "properties": {
                    "files": {
                      "type": "object",
                      "description": "File tree structure compatible with WebContainer",
                      "additionalProperties": {
                        "${'$'}ref": "#/definitions/fileEntry"
                      },
                      "required": ["package.json", "index.html", "server.js"]
                    }
                  },
                  "required": ["files"]
                }
              },
              "definitions": {
                "fileEntry": {
                  "oneOf": [
                    {
                      "type": "string",
                      "additionalProperties": false
                    },
                    {
                      "type": "object",
                      "properties": {
                        "files": {
                          "type": "object",
                          "description": "Nested file tree for a directory",
                          "additionalProperties": {
                            "${'$'}ref": "#/definitions/fileEntry"
                          }
                        }
                      },
                      "required": ["files"],
                      "additionalProperties": false
                    }
                  ]
                }
              }
            }
            "strict": true
        }
        """.trimIndent()
}
