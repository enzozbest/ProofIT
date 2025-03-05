package kcl.seg.rtt.prompting.helpers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

object PromptingTools {
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
        You are a software engineer tasked with generating functional requirements for a software prototype formatted for WebContainers.
        You must respond with **a single valid JSON object**, containing nothing else: no explanations, preambles, or additional text.
        The first character of your response must be "{" and the last character "}". You must not use the phrase "\n" anywhere in your response.
        You must generate functional requirements for all functionality you think the user wants based on their prompt given below.
        Functional requirements must include making the prototype interactive from the very start, with dummy values where needed.
        Functional requirements are **Strings** of text that describe one piece of functionality that the software must have. 
        They must be formatted as strings, and must not be anything else.
        Do not include the phrase "\n" anywhere in your response.
        
        ### JSON Structure:
        -A key-value pair where the key is `"requirements"` and the value is a list of functional requirements.
        -A key-value pair where the key is `"keywords"` and the value is a list of relevant keywords from the functional requirements you generated.
        
        Now, generate a JSON response for the following request:
        
        **User Request:**
        "$prompt"
        --KEYWORDS--
        "$keywords"
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
        You are a programmer that generates software prototypes formatted for WebContainers.  
        You must respond with **a single valid JSON object**. You must not include anything else in your response:
        no explanations, preambles, additional text, or formatting character sequences. 
                    
        If you decide to create a multi page website, you must provide only one file with all HTML CSS and JS. 
        Different pages must be represented by different divs, each with a class of "page". Only one div must have a class of "active". 
        You must not not include "\n" anywhere in your response.
        You must not include ANY comments in any part of your answer. Your code must be completely uncommented and undocumented.
        You should generate the code from the functional requirements given below, as well as the semantics of the user prompt. 
        You must include dummy values wherever needed to allow immediate testing.
        
        Different pages must be represented by different divs with a class of "page" and only one div with a class of "active" 
        Do not include "\n" in the files
        You should generate the code from the functional requirements in the user prompt and include dummy values to allow immediate testing

        ### JSON Structure:
        - `"code"`: The code for the prototype. It must be compatible with WebContainers.
        - `"requirements"`: The functional requirements your code fulfils.
        - `"mainFile"`: Specifies the main entry language (e.g., `"html"`).
        - `"files"`: An object where each key is a language identifier and the value is the corresponding code:
        - `"package.json"`: Must be included with all required dependencies.
        - Ensure that:
        - All scripts use `"npm start"` for execution.
        - Static files (if any) are served correctly.

        - Common languages include: 
        - "html", "css", "js", "python", "typescript", "php", etc.
        
        Example response format (but not limited to these languages):
        ```json
        {
        "mainFile": "A counter application with increment and decrement buttons",
        "files": {
            "html": "<html>...</html>",
            "css": "body { ... }",
            "js": "document.addEventListener('DOMContentLoaded', function() { ... });",
            "package.json": "{ \"name\": \"counter-app\", \"dependencies\": {...} }"
        }
        }
        ```
        
        Now, generate a JSON response for the following functional requirements:

        **User Request:** "$userPrompt"   
        **Requirements:** "$requirements"
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
            val noNewLines = response.replace("\\n", "")
            Json.decodeFromString<JsonObject>(noNewLines) // Attempt to return the response as is.
        }.getOrElse {
            val cleaned = cleanLlmResponse(response) // If it fails, clean the response first and try again.
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
        return jsonString.trim().replace("\\n", "")
    }
}
