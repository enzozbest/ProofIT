package kcl.seg.rtt.prompting.helpers

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
        You are an AI that generates functional requirements for a software protoype, formatted for WebContainers.
        Your response must be **a single valid JSON object** and contain nothing else—no explanations, preambles, or additional text.
        You should generate functional requirements, ensuring to add all functionality you think the user would want and make it interactive from the very start, even with dummy values.
        Do not include the phrase "\n" in your list of functional requirements
        
        ### JSON Structure:
        - `"mainFile"`: Contains a string, which is a numbered list of the functional requirements
        - `"files"`: This must  **always** be included in your response and is just an empty List
        
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
        You are an AI that generates software prototypes formatted for WebContainers.  
        Your response must be **a single valid JSON object** and contain nothing else—no explanations, preambles, or additional text. 
                    
        Different pages must be represented by different divs with a class of "page" and only one div with a class of "active" 
        Do not include "\n" in the files
        You should generate the code from the functional requirements in the user prompt and include dummy values to allow immediate testing

        ### JSON Structure:
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
}
