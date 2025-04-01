package prototype.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse
import prototype.helpers.parseOpenAIResponse
import utils.environment.EnvironmentLoader

/**
 * Service for interacting with OpenAI API
 */
object OpenAIService : LLMService {
    /**
     * Sends a prompt to the language model and returns the generated response.
     *
     * @param prompt The text prompt to send to the language model
     * @param model The identifier of the language model to use
     * @param options Options for controlling the behavior of the language model
     * @return The response from the language model, or null if the request failed
     */
    override suspend fun generateResponse(
        prompt: String,
        model: String,
        options: LLMOptions,
    ): Result<LLMResponse?> {
        if (options !is OpenAIOptions) {
            return Result.failure(Exception("Invalid options type for OpenAIService"))
        }

        val request =
            buildOpenAIRequest(
                apiHost = EnvironmentLoader.get("OPENAI_HOST"),
                apiPath = EnvironmentLoader.get("OPENAI_PATH"),
                apiKey = EnvironmentLoader.get("OPENAI_API_KEY"),
                organisationId = EnvironmentLoader.get("OPENAI_ORGANISATION_ID"),
                projectId = EnvironmentLoader.get("OPENAI_PROJECT_ID"),
                model = model,
                prompt = prompt,
                instructions = generateInstructions(),
            )

        return try {
            val response = callOpenAI(request, options)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to call OpenAI: ${e.message}"))
        }
    }

    /**
     * Makes a call to OpenAI API and parses the response.
     *
     * @param request The HTTP request to send to OpenAI
     * @param options Options for controlling the behavior of the language model
     * @return An instance of [OpenAIResponse] containing the API's response, or null if parsing fails
     */
    suspend fun callOpenAI(
        request: HttpRequestBuilder,
        options: OpenAIOptions,
    ): OpenAIResponse? {
        val client = HttpClient(CIO)
        val response = client.request(request)
        println(response.bodyAsText())
        return parseOpenAIResponse(response.bodyAsText())
    }

    /**
     * Builds an HTTP request for the OpenAI API.
     *
     * @param apiHost The host of the OpenAI API
     * @param apiPath The path of the OpenAI API
     * @param apiKey The API key for authentication
     * @param organisationId The organisation ID for authentication
     * @param projectId The project ID for authentication
     * @param model The model to use for generation
     * @param prompt The prompt to send to the model
     * @param instructions Additional instructions for the model
     * @return A configured HttpRequestBuilder
     */
    private fun buildOpenAIRequest(
        apiHost: String,
        apiPath: String,
        apiKey: String,
        organisationId: String,
        projectId: String,
        model: String,
        prompt: String,
        instructions: String,
    ): HttpRequestBuilder =
        HttpRequestBuilder()
            .apply {
                method = HttpMethod.Post
                url {
                    protocol = URLProtocol.HTTPS
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
                            put("instructions", JsonPrimitive(instructions))
                        }
                    }
                setBody(Json.encodeToString(JsonObject.serializer(), jsonRequestObject))
            }

    /**
     * Generates instructions for the OpenAI API.
     *
     * @return A string containing the instructions for the OpenAI API
     */
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
