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
                    
        If you decide to create multiple pages, each page must be represented by a different div with a class of "page".
        Only one div must have a class of "active". 
        You must not not include "\n" anywhere in your response.
        You must not include ANY comments in any part of your answer. Your code must be completely uncommented and undocumented.
        You should generate the code from the functional requirements given below, as well as the semantics of the user prompt. 
        You must include dummy values wherever needed to allow immediate testing.
        You are free to structure the code as you wish. You may use any programming languages, libraries, or frameworks.
        Common languages include HTML, CSS, JavaScript, and JSON, Python, Java, Kotlin, C++, C.
        Common frameworks for JavaScript include React, Angular, Vue, Express, Bootstrap, Tailwind, ect.
        Common frameworks for Python include Django, Flask, etc,
        Common frameworks for Java include Spring, etc.
        A common framework for C++ is Qt.
        A common framework for Kotlin is Ktor.
        You need not be constrained by those, you may wish to use other languages or frameworks.
        
        ### JSON Structure:
        - `"requirements"`: The functional requirements your code fulfils.
        - `"mainFile"`: Specifies the main entry language (e.g., `"html"`).
        - `"files"`: An object containing the following:
            - For each language used, a key-value pair in which a key is a language identifier and the value is an object containing:
                -A key-value pair in which the key is "code" and the value the corresponding code.
                - A key-value pair in which the key is "frameworks" and the value is a list of frameworks used, if any.
                - A key-value pair in which the key is "dependencies" and the value is a list of dependencies used, if any.
            
        Example response format:
        ```json
        {
         "mainFile": "hmtl",
            "files": {
                "html": {
                    "code": "<html>...</html>"
                    "frameworks": [],
                    "dependencies": [],
                },
                "css": {
                    "code": !body { ... }",
                    "frameworks": ["Tailwind"],
                    "dependencies": [],
                },
                "JavaScript": {
                    "code": "document.addEventListener('DOMContentLoaded', function() { ... });",
                    "frameworks": ["React"],
                    "dependencies": ["React"],
                 }
            }
        }
        ```
        
        Now, generate a JSON response for the following user prompt and functional requirements.

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
