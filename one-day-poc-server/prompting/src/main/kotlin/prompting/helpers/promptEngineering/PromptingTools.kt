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
            Respond with a single valid JSON object only. No explanations, comments, or additional text.

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
            You are an expert software architect specializing in creating high-quality,
            production-ready prototypes for WebContainers.

            ### Response Format
            Respond with a single valid JSON object only. No explanations, comments, or additional text.

            ### Response Structure
            Your response must strictly obey the schema provided below.
            Schema:
            {
              "${'$'}schema": "http://json-schema.org/draft-07/schema#",
              "title": "Ollama Response Schema",
              "type": "object",
              "properties": {
                "chat": {
                  "type": "object",
                  "description": "Chat response from the server",
                  "properties": {
                    "message": {
                      "type": "string",
                      "description": "The content of the message"
                    },
                    "role": {
                      "type": "string",
                      "enum": ["User", "LLM"],
                      "description": "The role of the message sender"
                    },
                    "timestamp": {
                      "type": "string",
                      "format": "date-time",
                      "description": "ISO 8601 timestamp of when the message was sent"
                    }
                  },
                  "required": ["message", "role", "timestamp"]
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
                      "type": "object",
                      "properties": {
                        "contents": {
                          "type": "string",
                          "description": "File contents as a string"
                        }
                      },
                      "required": ["contents"],
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

            ### Code Generation Rules
            1. Architecture:
               1. Follow SOLID principles.
               2. Use clean architecture patterns.
               3. Implement proper separation of concerns.
               4. Ensure modularity and reusability where possible.

            2. User Interface:
               1. Must implement responsive design.
               2. Must follow accessibility standards (WCAG 2.1).
               3. Must ensure consistent styling.
               4. Pages must use <div class="page"> with only one having class="active" as well.

            3. Code Standards:
               1. Write clean, self-documenting code (no comments allowed anywhere).
               2. Use meaningful variable/function names.
               3. Follow language-specific best practices.
               4. Implement proper error handling.
               5. Include input validation
               6. Use type safety where applicable.

            4. Interactivity:
               1. Add event listeners for user interactions.
               2. Implement immediate feedback mechanisms.
               3. Include loading states.
               4. Handle edge cases.
               5. Use dummy data for immediate testing.

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
               2. Provided functional requirements.
               3. Available reference templates.
               4. Modern development best practices.
               5. Modern styling practices. You must style the components to make them visually appealing using
                  whatever styling framework you choose.
               6. If templates are provided, your styling must be consistent with the provided templates.
               You must provide a complete answer, with all necessary files and dependencies, including the version
               of the dependencies. Code must be written in full, not just placeholders or using the phrase "...".
            """.trimIndent()

        val userMessage =
            """
            This is what I want you to generate a lightweight proof-of-concept prototype for:
            "$userPrompt"
            """.trimIndent()

        val functionalRequirementsMessage =
            """
            These are the functional requirements you should consider in addition to the user's message:
            $requirements
            """.trimIndent()

        val templatesMessage =
            """
            These are the templates you should consider (use them to help generate your response. DO NOT simply describe them):
            ${templates.joinToString(separator = "\n\n")}
            """.trimIndent()

        val finalPromptMessage =
            """
            Now produce the final JSON strictly following the schema. 
            
            Incorporate each reference template provided into its respective file in the prototype.files object. 
            Do not ignore the reference templates, rather extend/modify them to fit the requirements. 
            Ensure the final file structure must have package.json, index.html, and server.js at minimum,
            plus any other files needed to run this as a React app in WebContainer. For example: src/components/ChatInput.tsx. 
            Adjust the code from the templates to ensure they compile and run in the WebContainer environment. 
            Add dependencies in package.json for React, ReactDOM, Webpack/Vite, and anything else needed (e.g., ws for WebSockets). 
            The final code must run npm start without errors in WebContainer.
            
            Remember that the final JSON must include both a 'chat' key, with a simple, short message indicating what you have done,
            and the 'prototype' key, with the file structure of the prototype described previously.
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
                // Additional system-level instructions (functional requirements)
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", functionalRequirementsMessage)
                    },
                )
                // Additional system-level instructions (templates)
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
        val cleaned = cleanLlmResponse(response)
        println("CLEANED RESPONSE")
        return run {
            println("DECODING...")
            Json.decodeFromString<JsonObject>(cleaned)
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
                .replace(newLineRegex, "")
                .trim()
        return cleaned
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
