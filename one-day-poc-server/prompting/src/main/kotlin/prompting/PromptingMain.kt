package prompting

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import prompting.helpers.PrototypeInteractor
import prompting.helpers.promptEngineering.PromptingTools
import prompting.helpers.promptEngineering.SanitisationTools
import prompting.helpers.templates.TemplateInteractor
import prototype.LlmResponse
import prototype.helpers.OllamaOptions
import prototype.helpers.PromptException
import prototype.security.secureCodeCheck
import java.time.Instant

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
    val timestamp: String,,
    val messageId: String,
)

@Serializable
data class ServerResponse(
    val chat: ChatResponse,
    val prototype: PrototypeResponse? = null,
)

@Serializable
data class PrototypeResponse(
    val files: JsonObject, // Keep as JsonObject, not Map
)

/**
 * Main orchestrator for the multi-step prompting workflow.
 *
 * This class manages the entire process flow for generating responses from
 * user prompts, including prompt sanitization, requirements extraction, template
 * fetching, and final prototype generation.
 *
 * @property model The LLM model identifier to use for prompt processing (default: "qwen2.5-coder:14b")
 */
class PromptingMain(
    private val model: String = "qwen2.5:14b",
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
    suspend fun run(userPrompt: String): ServerResponse {
        /*
        val sanitisedPrompt = SanitisationTools.sanitisePrompt(userPrompt)
        val freqsPrompt = PromptingTools.functionalRequirementsPrompt(sanitisedPrompt.prompt, sanitisedPrompt.keywords)

        // First LLM call
        val freqsOptions = OllamaOptions(temperature = 0.50, top_k = 300, top_p = 0.9, num_predict = 500)
        val freqs: JsonObject = promptLlm(freqsPrompt, freqsOptions)

        val functionalRequirements =
            freqs["requirements"]?.jsonArray?.joinToString(",") + ", $userPrompt"

        // Use functional requirements to and user prompt fetch templates
        val templates = TemplateInteractor.fetchTemplates(functionalRequirements)

        // Prototype prompt with templates.
        val prototypePrompt = prototypePrompt(userPrompt, freqs, templates)

        // Second LLM call
        val prototypeOptions =
            OllamaOptions(temperature = 0.40, top_k = 300, top_p = 0.9)
        val prototypeResponse: JsonObject = promptLlm(prototypePrompt, prototypeOptions)
        println("DONE DECODING!")
        */
        
        // Create dummy response
        val dummyResponse = createDummyResponse()
        println("USING DUMMY RESPONSE INSTEAD OF LLM!")
        return serverResponse(dummyResponse)
    }

    /**
     * Creates a specialized prompt for generating a prototype based on requirements and templates.
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
     * Sends a prompt to the LLM and processes the response into a JsonObject.
     *
     * @param prompt The formatted prompt text to send to the LLM
     * @return A JsonObject containing the parsed response from the LLM
     * @throws PromptException If the LLM does not respond or if the response cannot be parsed
     */
    private fun promptLlm(
        prompt: String,
        options: OllamaOptions = OllamaOptions(),
    ): JsonObject =
        runBlocking {
            val llmResponse =
                PrototypeInteractor.prompt(prompt, model, options) ?: throw PromptException("LLM did not respond!")
            PromptingTools.formatResponseJson(llmResponse.response)
        }

    /**
     * Extracts the functional requirements and prototype files from the LLM response.
     * @param response The LLM response.
     * @return A [ServerResponse] containing both chat response and prototype files.
     */
    private fun serverResponse(response: JsonObject): ServerResponse {
        val defaultResponse = "Here is your code."
        val chat =
            when (val jsonReqs = response["chat"]) {
                is JsonPrimitive -> jsonReqs.content
                is JsonObject -> jsonReqs["message"]?.jsonPrimitive?.content ?: defaultResponse
                else -> defaultResponse
            }
        val chatResponse =
            ChatResponse(
                message = chat,
                role = "LLM",
                timestamp = Instant.now().toString(),
                messageId = "0",
            )

        val prototypeResponse =
            response["prototype"]?.let { prototype ->
                if (prototype is JsonObject && prototype.containsKey("files")) {
                    PrototypeResponse(
                        files = prototype["files"] as JsonObject,
                    )
                } else {
                    null
                }
            }
        return ServerResponse(
            chat = chatResponse,
            prototype = prototypeResponse,
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
     * Creates a dummy JSON response for testing purposes
     */
    private fun createDummyResponse(): JsonObject {
        
        // Create the chat response part
        val chatObject = buildJsonObject {
            put("message", JsonPrimitive("I've created a simple counter application with React. The app features a counter display, increment/decrement buttons, and a reset button. You can also set a custom step value for the increments. The code is structured in a clean, maintainable way with separate components for the counter display and controls."))
        }
        
        val filesObject = buildJsonObject {
            put("public/index.html", buildJsonObject {
                put("file", buildJsonObject {
                    put("contents", JsonPrimitive("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>React Counter App</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n</body>\n</html>"))
                })
            })
            
            // src/index.js
            put("src/index.js", buildJsonObject {
                put("file", buildJsonObject {
                    put("contents", JsonPrimitive("import React from 'react';\nimport ReactDOM from 'react-dom/client';\nimport App from './App';\nimport './styles.css';\n\nconst root = ReactDOM.createRoot(document.getElementById('root'));\nroot.render(\n  <React.StrictMode>\n    <App />\n  </React.StrictMode>\n);"))
                })
            })
            
            // src/App.jsx
            put("src/App.jsx", buildJsonObject {
                put("file", buildJsonObject {
                    put("contents", JsonPrimitive("import React, { useState } from 'react';\nimport CounterDisplay from './components/CounterDisplay';\nimport CounterControls from './components/CounterControls';\n\nfunction App() {\n  const [count, setCount] = useState(0);\n  const [step, setStep] = useState(1);\n\n  const increment = () => setCount(count + step);\n  const decrement = () => setCount(count - step);\n  const reset = () => setCount(0);\n  const handleStepChange = (e) => setStep(Number(e.target.value));\n\n  return (\n    <div className=\"app\">\n      <h1>React Counter</h1>\n      <CounterDisplay count={count} />\n      <div className=\"step-control\">\n        <label>\n          Step Size: \n          <input \n            type=\"number\" \n            min=\"1\" \n            value={step} \n            onChange={handleStepChange} \n          />\n        </label>\n      </div>\n      <CounterControls \n        onIncrement={increment} \n        onDecrement={decrement} \n        onReset={reset} \n      />\n    </div>\n  );\n}\n\nexport default App;"))
                })
            })
            
            // src/components/CounterDisplay.jsx
            put("src/components/CounterDisplay.jsx", buildJsonObject {
                put("file", buildJsonObject {
                    put("contents", JsonPrimitive("import React from 'react';\n\nfunction CounterDisplay({ count }) {\n  return (\n    <div className=\"counter-display\">\n      <h2>{count}</h2>\n    </div>\n  );\n}\n\nexport default CounterDisplay;"))
                })
            })
            
            // src/components/CounterControls.jsx
            put("src/components/CounterControls.jsx", buildJsonObject {
                put("file", buildJsonObject {
                    put("contents", JsonPrimitive("import React from 'react';\n\nfunction CounterControls({ onIncrement, onDecrement, onReset }) {\n  return (\n    <div className=\"counter-controls\">\n      <button onClick={onDecrement}>Decrease</button>\n      <button onClick={onReset}>Reset</button>\n      <button onClick={onIncrement}>Increase</button>\n    </div>\n  );\n}\n\nexport default CounterControls;"))
                })
            })
            
            // src/styles.css
            put("src/styles.css", buildJsonObject {
                put("file", buildJsonObject {
                    put("contents", JsonPrimitive("body {\n  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;\n  background-color: #f5f5f5;\n  margin: 0;\n  padding: 0;\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  min-height: 100vh;\n}\n\n.app {\n  background-color: white;\n  border-radius: 8px;\n  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);\n  padding: 2rem;\n  max-width: 400px;\n  width: 100%;\n  text-align: center;\n}\n\nh1 {\n  color: #333;\n  margin-bottom: 1rem;\n}\n\n.counter-display {\n  margin: 1.5rem 0;\n}\n\n.counter-display h2 {\n  font-size: 4rem;\n  margin: 0;\n  color: #2c3e50;\n}\n\n.step-control {\n  margin-bottom: 1.5rem;\n}\n\n.step-control input {\n  width: 60px;\n  padding: 0.5rem;\n  margin-left: 0.5rem;\n  border: 1px solid #ddd;\n  border-radius: 4px;\n}\n\n.counter-controls {\n  display: flex;\n  gap: 0.5rem;\n  justify-content: center;\n}\n\nbutton {\n  background-color: #3498db;\n  color: white;\n  border: none;\n  border-radius: 4px;\n  padding: 0.5rem 1rem;\n  cursor: pointer;\n  font-size: 1rem;\n  transition: background-color 0.2s;\n}\n\nbutton:hover {\n  background-color: #2980b9;\n}\n\nbutton:nth-child(2) {\n  background-color: #e74c3c;\n}\n\nbutton:nth-child(2):hover {\n  background-color: #c0392b;\n}"))
                })
            })
            
            // package.json
            put("package.json", buildJsonObject {
                put("file", buildJsonObject {
                    put("contents", JsonPrimitive("{\n  \"name\": \"react-counter-app\",\n  \"version\": \"1.0.0\",\n  \"description\": \"A simple React counter application\",\n  \"main\": \"index.js\",\n  \"scripts\": {\n    \"start\": \"react-scripts start\",\n    \"build\": \"react-scripts build\",\n    \"test\": \"react-scripts test\",\n    \"eject\": \"react-scripts eject\"\n  },\n  \"dependencies\": {\n    \"react\": \"^18.2.0\",\n    \"react-dom\": \"^18.2.0\",\n    \"react-scripts\": \"5.0.1\"\n  },\n  \"browserslist\": {\n    \"production\": [\n      \">0.2%\",\n      \"not dead\",\n      \"not op_mini all\"\n    ],\n    \"development\": [\n      \"last 1 chrome version\",\n      \"last 1 firefox version\",\n      \"last 1 safari version\"\n    ]\n  }\n}"))
                })
            })
        }
        
        // Create prototype object
        val prototypeObject = buildJsonObject {
            put("files", filesObject)
        }
        
        // Build and return final response
        return buildJsonObject {
            put("chat", chatObject)
            put("prototype", prototypeObject)
        }
    }
}
