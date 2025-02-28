package kcl.seg.rtt.prompting.helpers

object PromptingTools {
    fun functionalRequirementsPrompt(
        prompt: String,
        keywords: List<String>,
    ): String =
        """
        You are an AI that generates software prototypes formatted for WebContainers.
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
                    
        If you are creating a multi page website, please provide only one file with all HTML CSS and JS 
        Different pages must be represented by different divs with a class of "page" and only one div with a class of "active" 
        Do not include "\n" in the files
        You should generate the code off the functional requirements in the user prompt and include dummy values to allow immediate testing
        
        ### JSON Structure:
        - `"mainFile"`: Specifies the main entry file (e.g., `"index.js"`).
        - `"files"`: An object where each key is a filename and the value is an object containing:
        - `"content"`: The full content of the file.
        - `"package.json"`: Must be included with all required dependencies.
        - Ensure that:
        - All scripts use `"npm start"` for execution.
        - Static files (if any) are served correctly.
        
        Now, generate a JSON response for the following functional requirements:

        **User Request:** "$userPrompt"   
        **Requirements:** "$requirements"
        **Templates:** "$templates"
        """.trimIndent()
}
