package prompting.helpers.promptEngineering

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PromptingTools {
    private val newLineRegex = Regex("(\\n|\\\\n)")

    /**
     * Extracts functional requirements from a user's prompt and associated keywords
     *
     * @param prompt the original user's input.
     * @param keywords Extracted from the original input.
     * @return Formatted prompt with system instructions
     */
    fun functionalRequirementsPrompt(
        prompt: String,
        keywords: List<String>,
    ): String {
        val systemMessage =
            """
            You are an expert software requirements engineer tasked with generating precise, actionable functional 
            requirements for a software prototype that will run in WebContainers.

            ### Response Format
            Respond with a single valid JSON object only. The JSON must be parseable. 
            Do not include any explanations, comments, or additional text.

            ### Response Structure
            Your response must strictly follow the example below. It must be a single valid JSON object containing 
            only the "requirements" and "keywords" fields. 
            The "requirements" field must be an array of strings, each representing one of your generated functional requirements.
            The "keywords" field must be an array of strings, each representing a relevant keyword you identified
            in your functional requirements.
            
            Example: 
            {
                "requirements": [
                    "The system shall display a login form with email and password fields",
                    "The system shall validate email format before form submission",
                    "The system shall provide error feedback for invalid inputs"
                ],
                "keywords": ["authentication", "validation", "user feedback"]
            }

            ### Requirements Rules  
            1. Functional Requirements must be:
               1. Specific, measurable, and testable.
               2. Self-contained (one requirement = one functionality).
               3. Implementation-independent.
               4. Written in active voice.
               5. Clear and unambiguous.

            2. Each requirement must follow this pattern:
               1. Start with "The system shall...".
               2. Describe a single, atomic functionality.
               3. Specify user interactions and expected system responses, where applicable.

            ### Your Task
            Generate comprehensive functional requirements based on:
            1. The user's request.
            2. The provided keywords.
            3. Industry best practices for similar systems.
            4. Common user expectations.
            """.trimIndent()

        val userMessage =
            """
            This is what I want you to generate functional requirements for:
            "$prompt"
            """.trimIndent()

        val keywordsMessage =
            """
            These are the keywords you should consider in addition to the user's message:
            ${keywords.joinToString(", ")}
            """.trimIndent()

        val finaliser =
            """
            Now, generate the final JSON response.
            """.trimIndent()

        val jsonArary =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemMessage)
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", userMessage)
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", keywordsMessage)
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", finaliser)
                    },
                )
            }

        return Json.encodeToString<JsonArray>(jsonArary)
    }

    /**
     * Creates a prompt combining functional requirements with system instructions for WebContainers format
     *
     * @param userPrompt the original user input.
     * @param requirements generated from the original user input via a call to [functionalRequirementsPrompt].
     * @param templates list of templates to include in the prompt, if available.
     * @return Formatted prompt with system instructions.
     */
    fun prototypePrompt(
        userPrompt: String,
        requirements: String,
        templates: List<String>,
    ): String {
        val systemMessage =
            """
            The model is an expert software engineer specializing in creating high-quality, production-ready prototypes
            for WebContainers. The model must answer according to the provided functional requirements and templates. The model's job
            is to extend/modify/combine the given templates together to fit the functional requirements and create a
            full working solution.

            ### Response Format
            The model always responds with a single valid JSON object only. The model never includes any explanations,
            comments, or additional text. 
            THE MODEL MUST NEVER USE BACKTICKED STRINGS (`) IN ITS RESPONSE. THE MODEL MUST ALWAYS USE ONLY VALID JSON NOTATION.

            ### Response Structure
            The model's response must strictly obey the schema and examples provided (to follow). The model is
            not allowed to change this format in any way.
            
            Schema:
            {
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
            Good Example:
            {
                "chat" : "This is a sample message to go in the chat"
                "prototype": {
                    "files": {
                        "package.json": "{
                        "contents": "{ \"name\": \"prototype\", \"version\": \"1.0.0\" }",
                    }
                }
            }
            
            The model must adhere to this strictly, no other response format is allowed. The model's response must include
             both the 'chat' and 'prototype' keys at the top-level and only those. 

            ### Code Generation Rules
            1. Pages the model generates must use <div class="page">. Only one of those must have class="active" as well.
            2. The model always ensures modularity and reusability where possible.
            3. The model always implements responsive user interface design.
            4. The model always follows accessibility standards (WCAG 2.1).
            5. The model always ensures consistent styling throughout
            6. Write clean, self-documenting code (no comments allowed anywhere). 
            7. Implement proper error handling.
            8. Include input validation.
            9. Use type safety where applicable.
            10. Add event listeners for user interactions.
            11. Implement immediate feedback mechanisms.
            12. Use dummy data for immediate experimentation.
            
            ### Technology Stack
            Choose appropriate technologies from:
            1. Frontend: HTML5, CSS3, JavaScript (ES6+), TypeScript.
            2. Frameworks: React, Vue, Angular, Svelte.
            3. Styling: Tailwind, Bootstrap, Material-UI.
            4. Backend: Node.js.
            5. Backend Frameworks: Express, Spring.

            ### Your Task
            Generate production-quality code based on:
            1. The user's message.
            2. The provided functional requirements (to follow).
            3. Available reference templates (to follow).
            5. Modern styling practices. You must style the components to make them visually appealing using
            whatever styling framework you choose.
            """.trimIndent()

        val userMessage =
            """
            This is what I want you to generate a lightweight proof-of-concept prototype for:
            "$userPrompt"
            """.trimIndent()

        val functionalRequirementsMessage =
            """
            You must consider the following functional requirements in addition to the user's message:
            $requirements
            """.trimIndent()

        val templatesMessage =
            """
            These are the templates you must consider (use them to help generate your response. DO NOT simply describe them):
            ${templates.joinToString(separator = "\n\n")}
            """.trimIndent()

        val finalPromptMessage =
            """
            Now produce the final JSON strictly following the schema.
            
            Incorporate each reference template provided into its respective file in the prototype.files object. 
            Do not ignore the reference templates, rather extend/modify/combine them to fit the functional requirements. 
            Adjust the code from the templates to ensure they compile and run in the WebContainer environment. 
            Add dependencies in package.json for React, ReactDOM, Webpack/Vite, and anything else needed (e.g., ws for WebSockets). 
            The final code must run `npm install` and `npm start` without errors in a WebContainer.
            
            The final JSON must include:
             1. A 'chat' key, with a simple message indicating how your solution meets the user's original prompt.
             2.A 'prototype' key, with the file structure of the prototype. 
             
            Check your response before finalising it. If it is not formatted correct, make the necessary changes
            before sending it.
            
            Now produce your response.
            """.trimIndent()

        val messagesArray =
            buildJsonArray {
                // Main system constraints
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemMessage)
                    },
                )
                // The user's initial request
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", userMessage)
                    },
                )
                // Additional system-level functional requirements
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", functionalRequirementsMessage)
                    },
                )
                // Additional system-level templates
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", templatesMessage)
                    },
                )
                // Final "user" request: produce the final JSON response
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", finalPromptMessage)
                    },
                )
            }

        return Json.encodeToString(JsonArray.serializer(), messagesArray)
    }

    /**
     * Formats the response from the LLM to remove new line characters and ensure it is valid JSON.
     * If the JSON is invalid initially, it will attempt to clean the response and try again.
     *
     * @param response The raw response from the LLM as a string
     * @return The formatted JSON response as a JsonObject
     */
    fun formatResponseJson(response: String): JsonObject {
        val cleaned = cleanLlmResponse(response).also { println(it) }
        println("CLEANED RESPONSE")
        return run {
            println("DECODING...")
            runCatching { Json.decodeFromString<JsonObject>(cleaned) }.getOrElse {
                println(it)
                error("ERROR: $it")
            }
        }
    }

    /**
     * Extracts and cleans a JSON object from an LLM response string.
     *
     * Identifies the first opening '{' and last closing '}' brace to extract the JSON object,]
     * then removes comments, handles escaped quotations, and normalizes whitespace.
     *
     * @param response The raw string from an LLM that may contain a JSON object
     * @return A cleaned string containing only the JSON object ready for parsing
     */
    private fun cleanLlmResponse(response: String): String {
        val openingBrace = response.indexOf('{')
        val closingBrace =
            response.length -
                response
                    .reversed()
                    .indexOf('}') // As a "forwards" index pointing to the character just after the last '}'

        val jsonString = response.substring(openingBrace, closingBrace)
        println(jsonString)
        val cleaned =
            jsonString
                .removeComments()
                .removeEscapedQuotations()
                .replace(newLineRegex, "")
                .trim()
        return cleaned
    }

    fun String.removeEscapedQuotations(): String {
        val pattern = Regex("\\\"")
        return this.replace(pattern, "\"")
    }

    /**
     * Removes C-style comments from a string.
     *
     * Uses regex with careful pattern matching to avoid false positives like URLs.
     *
     * @receiver String containing potential comments
     * @return String with all comments removed
     */
    fun String.removeComments(): String {
        val cStyleCommentRegex = Regex("""(?<!:)//.*?\\n|/\*[\s\S]*?\*/""", RegexOption.MULTILINE)
        val pythonStyleCommentRegex = Regex("""#.*?(?:\\n|$)""", RegexOption.MULTILINE)
        return this.replace(cStyleCommentRegex, "").replace(pythonStyleCommentRegex, "")
    }
}
