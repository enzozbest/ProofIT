package kcl.seg.rtt.prompting.helpers

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
        Respond with a single valid JSON object only. No explanations, comments, or additional text.

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

        ### JSON Structure Example
        {
            "requirements": [
                "The system shall display a login form with email and password fields",
                "The system shall validate email format before form submission",
                "The system shall provide error feedback for invalid inputs"
            ],
            "keywords": ["authentication", "validation", "user feedback"]
        }

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
        templates: String,
    ): String =
        """
        You are an expert software architect specializing in creating high-quality, production-ready prototypes for WebContainers.

        ### Response Format
        Provide a single valid JSON object. No additional text, comments, or explanations.

        ### Code Quality Requirements
        1. Architecture:
           - Follow SOLID principles
           - Use clean architecture patterns
           - Implement proper separation of concerns
           - Ensure modularity and reusability

        2. User Interface:
           - Implement responsive design
           - Follow accessibility standards (WCAG 2.1)
           - Use semantic HTML elements
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
        - Backend: Node.js, Python, Java, Kotlin, C++
        - Backend Frameworks: Express, Django, Spring, Ktor
        - Testing: Jest, Cypress, JUnit, PyTest

        ### JSON Structure
        {
            "requirements": [
                "The system implements user authentication with email/password"
            ],
            "mainFile": "html",
            "files": {
                "html": {
                    "code": "<div class='page active'>...</div>",
                    "frameworks": ["React"],
                    "dependencies": ["react", "react-dom"]
                },
                "css": {
                    "code": ".page { ... }",
                    "frameworks": ["Tailwind"],
                    "dependencies": []
                },
                "javascript": {
                    "code": "const App = () => { ... }",
                    "frameworks": ["React"],
                    "dependencies": ["axios"]
                }
            }
        }

        ### Your Task
        Generate production-quality code based on:
        1. User requirements below
        2. Provided functional requirements
        3. Available templates
        4. Modern development best practices
        You must provide a complete answer, with all necessary files and dependencies. Code must be written in 
        full, not just placeholders or using the phrase "...".

        **User Prompt:** "$userPrompt"   
        **Functional Requirements:** "$requirements"
        **Templates:** "$templates"
        """.trimIndent()

    /**
     * Formats the response from the LLM to remove new line characters and ensure it is valid JSON.
     * If the JSON is invalid initially, it will attempt to clean the response and try again.
     *
     * @param response The raw response from the LLM as a string
     * @return The formatted JSON response as a JsonObject
     */
    fun formatResponseJson(response: String): JsonObject =
        runCatching {
            val noNewLines = response.removeComments().replace(newLineRegex, "")
            Json.decodeFromString<JsonObject>(noNewLines) // Attempt to return the response as is.
        }.getOrElse {
            val cleaned = cleanLlmResponse(response)
            println(cleaned) // If it fails, clean the response first and try again.
            Json.decodeFromString<JsonObject>(cleaned)
        }

    /**
     * Cleans a string by removing any leading or trailing characters that are not part of the JSON object.
     */
    private fun cleanLlmResponse(response: String): String {
        val openingBrace = response.indexOf('{')
        val closingBrace =
            response.length -
                response
                    .reversed()
                    .indexOf('}') // As a "forwards" index pointing to the character just after the last '}'

        val jsonString = response.substring(openingBrace, closingBrace)
        val cleaned = jsonString.removeComments().replace(newLineRegex, "").trim()
        return cleaned
    }

    /**
     * Removes comments from a string. This includes C-style comments (// and /* */) and Python-style comments (#).
     */
    private fun String.removeComments(): String {
        val cStyleCommentRegex = Regex("""(//.*?$|/\*[\s\S]*?\*/)""", RegexOption.MULTILINE)
        val pythonStyleCommentRegex = Regex("""#.*$""")
        return this.replace(cStyleCommentRegex, "").replace(pythonStyleCommentRegex, "")
    }
}
