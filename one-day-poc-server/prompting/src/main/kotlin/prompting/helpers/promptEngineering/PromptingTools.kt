package prompting.helpers.promptEngineering

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

object PromptingTools {
    private val newLineRegex = Regex("(\\n|\\\\n)")

    /**
     * Creates functional requirements following the user prompt and extracted keywords
     *
     * @param prompt Generated from the original user input
     * @param keywords Extracted from the original user input
     * @return Formatted prompt with system instructions
     */
    fun functionalRequirementsPrompt(
        prompt: String,
        keywords: List<String>,
    ): String =
        """
        You are an expert software requirements engineer tasked with generating precise, actionable functional requirements for a software prototype that will run in WebContainers.

        ### Response Format
        Respond with a single valid JSON object only, according to the example below. No explanations, comments, or additional text.
        Example: {
            "requirements": [
                "The system shall display a login form with email and password fields",
                "The system shall validate email format before form submission",
                "The system shall provide error feedback for invalid inputs"
            ],
            "keywords": ["authentication", "validation", "user feedback"]
        }

        ### Requirements Guidelines
        1. Requirements must be:
           - Specific, measurable, and testable
           - Self-contained (one requirement = one functionality)
           - Implementation-independent
           - Written in active voice
           - Clear and unambiguous

        2. Each requirement must follow this pattern:
           - Start with "The system shall..."
           - Describe a single, atomic functionality
           - Include acceptance criteria where applicable
           - Specify user interactions and expected system responses

        3. Interactive Elements:
           - Every UI component must have associated user interactions
           - Include data validation rules where applicable
           - Specify error handling and feedback mechanisms
           - Define initial/dummy values for immediate testing

        ### Your Task
        Generate comprehensive requirements based on:
        1. The user's request below
        2. The provided keywords
        3. Industry best practices for similar systems
        4. Common user expectations

        **User Request:**
        "$prompt"

        **Keywords:**
        $keywords
        """.trimIndent()

    /**
     * Creates a prompt combining functional requirements with system instructions for WebContainers format
     *
     * @param requirements Generated from the original user input
     * @return Formatted prompt with system instructions
     */
    fun prototypePrompt(
        userPrompt: String,
        requirements: String,
        templates: List<String>,
    ): String =
        $$"""
        ### Your response must obey the following JSON Schema. Do not copy the schema into your response. DO NOT USE BACKTICKED STRINGS, USE ONLY VALID JSON NOTATION. IF YOU NEED A NEW LINE, USE "\n".
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "title": "Server Response Schema",
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
                  "additionalProperties": { "${'$'}ref": "#/definitions/fileEntry" },
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
                      "additionalProperties": { "${'$'}ref": "#/definitions/fileEntry" }
                    }
                  },
                  "required": ["files"],
                  "additionalProperties": false
                }
              ]
            }
          }
        }
           
        You are an expert software architect specializing in creating high-quality, production-ready prototypes for WebContainers.

        ### Code Quality Requirements
        1. Architecture:
           - Follow SOLID principles
           - Use clean architecture patterns
           - Implement proper separation of concerns
           - Ensure modularity and reusability

        2. User Interface:
           - Implement responsive design
           - Follow accessibility standards (WCAG 2.1)
           - Ensure consistent styling
           - Pages must use <div class="page"> with one having class="active"

        3. Code Standards:
           - Write clean, self-documenting code (no comments needed)
           - Use meaningful variable/function names
           - Follow language-specific best practices
           - Implement proper error handling
           - Include input validation
           - Use type safety where applicable

        4. Interactivity:
           - Add event listeners for user interactions
           - Implement immediate feedback mechanisms
           - Include loading states
           - Handle edge cases
           - Use dummy data for immediate testing

        ### Technology Stack
        Choose appropriate technologies from:
        - Frontend: HTML5, CSS3, JavaScript (ES6+), TypeScript
        - Frameworks: React, Vue, Angular, Svelte
        - Styling: Tailwind, Bootstrap, Material-UI
        - Backend: Node.js
        - Backend Frameworks: Express, Spring

        ### Your Task
        Generate production-quality code based on:
        1. User prompt in natural language below
        2. Provided functional requirements
        3. Available templates provided below
        4. Modern development best practices
        5. Modern styling practices. You must style the components to make them visually appealing using whatever styling framework you choose.
        6. If templates are provided, your styling must be consistent with the provided templates.
        You must provide a complete answer, with all necessary files and dependencies, including the version of the dependencies. Code must be written in 
        full, not just placeholders or using the phrase "...".

        **User Prompt:** "$$userPrompt"   
        **Functional Requirements:** "$$requirements"
        **Templates:** "$$templates"
        """.trimIndent()

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
