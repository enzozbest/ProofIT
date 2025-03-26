package prompting.helpers.promptEngineering

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PromptingTools {
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
            The model is an expert software requirements engineer tasked with generating precise, actionable functional 
            requirements for a software prototype that will run in WebContainers.

            ### Response Format
            The model always responds with a single valid JSON object only. The JSON must be parseable automatically. 
            The model never includes any explanations, comments, or additional text in its response.

            ### Response Structure
            The model's response must strictly follow the example given. The model's response
            must be a single valid JSON object containing only the "requirements" and "keywords" keys. 
            The "requirements" key must have as its value an array of strings, each representing one of the model's generated functional requirements.
            The "keywords" key must have as its value an array of strings, each representing a relevant keyword the model identified
            in the functional requirements or the user's prompt.
            
            Example: 
            {
                "requirements": [
                    "The system shall display a login form with email and password fields",
                    "The system shall validate email format before form submission",
                    "The system shall provide error feedback for invalid inputs"
                ],
                "keywords": ["authentication", "validation", "user feedback"]
            }

            ### What requirements must be like  
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

            ### What the model must do
            Generate comprehensive functional requirements based on:
            1. The user's prompt.
            2. The provided keywords.
            3. Industry best practices for similar systems.
            4. Common user expectations, based on the semantic meaning of their prompt.
            """.trimIndent()

        val userMessage =
            """
            I want the model to generate functional requirements for a system that will run in WebContainers with the following
            high-level specification:
            "$prompt"
            """.trimIndent()

        val keywordsMessage =
            """
            The model will now receive the keywords the model should consider in addition to the user's prompt:
            ${keywords.joinToString(", ")}
            """.trimIndent()

        val finaliser =
            """
            Now, the model will generate the final JSON response.
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
        previousGeneration: String? = null,
    ): String {
        val systemMessage =
            """
            The model is an expert software engineer specializing in creating high-quality, production-ready lightweight 
            proof-of-concept prototypes that run in WebContainers. The model must answer based on 
            the provided functional requirements and templates. The model's job is to extend/modify/combine the given 
            templates together to fit the functional requirements and create a full working solution.

            ### Response Format
            The model always responds with a single valid JSON object only. The model never includes any explanations,
            comments, or additional text. 

            ### Response Structure
            The model's response must strictly obey the schema and examples provided. The model is
            not allowed to change this format in any way.
            
            Example (your response must look like this):
            {
              "chat": "I've created a React counter application using Vite as the build tool. The app includes proper JSX handling for all JavaScript files, ensuring compatibility with React's syntax. I've also included a vite.config.js file that configures esbuild to properly process JSX in .js files, preventing common build errors.",
              "prototype": {
                "files": {
                  "package.json": {
                    "name": "react-counter-app",
                    "version": "1.0.0",
                    "description": "A simple React counter application using Vite",
                    "scripts": {
                      "start": "vite",
                      "build": "vite build",
                      "preview": "vite preview"
                    },
                    "dependencies": {
                      "react": "^18.2.0",
                      "react-dom": "^18.2.0"
                    },
                    "devDependencies": {
                      "@vitejs/plugin-react": "^4.0.0",
                      "vite": "^4.3.9"
                    }
                  },
                  "vite.config.js": "import { defineConfig } from 'vite';\nimport react from '@vitejs/plugin-react';\n\nexport default defineConfig({\n  plugins: [react()],\n  esbuild: {\n    loader: {\n      '.js': 'jsx',\n    },\n  },\n  server: {\n    port: 3000,\n    open: true\n  }\n});",
                  "index.html": "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\" />\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n  <title>React Counter App</title>\n</head>\n<body>\n  <div id=\"root\"></div>\n  <script type=\"module\" src=\"/src/main.js\"></script>\n</body>\n</html>",
                  "main.jsx": "import React from 'react';\nimport ReactDOM from 'react-dom/client';\nimport App from './App';\nimport './styles.css';\n\nReactDOM.createRoot(document.getElementById('root')).render(\n  <React.StrictMode>\n    <App />\n  </React.StrictMode>\n);",
                  "App.jsx": "import React, { useState } from 'react';\nimport Counter from './Counter';\n\nfunction App() {\n  return (\n    <div className=\"app\">\n      <h1>React Counter App</h1>\n      <Counter />\n    </div>\n  );\n}\n\nexport default App;",
                  "Counter.jsx": "import React, { useState } from 'react';\n\nfunction Counter() {\n  const [count, setCount] = useState(0);\n\n  const increment = () => setCount(count + 1);\n  const decrement = () => setCount(count - 1);\n  const reset = () => setCount(0);\n\n  return (\n    <div className=\"counter\">\n      <h2>Count: {count}</h2>\n      <div className=\"buttons\">\n        <button onClick={decrement}>-</button>\n        <button onClick={reset}>Reset</button>\n        <button onClick={increment}>+</button>\n      </div>\n    </div>\n  );\n}\n\nexport default Counter;",
                  "styles.css": "* {\n  box-sizing: border-box;\n  margin: 0;\n  padding: 0;\n}\n\nbody {\n  font-family: Arial, sans-serif;\n  line-height: 1.6;\n  background-color: #f5f5f5;\n  color: #333;\n}\n\n.app {\n  max-width: 600px;\n  margin: 2rem auto;\n  padding: 2rem;\n  background-color: white;\n  border-radius: 8px;\n  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);\n  text-align: center;\n}\n\nh1 {\n  margin-bottom: 2rem;\n  color: #444;\n}\n\n.counter {\n  margin: 2rem 0;\n}\n\n.counter h2 {\n  font-size: 2.5rem;\n  margin-bottom: 1.5rem;\n}\n\n.buttons {\n  display: flex;\n  justify-content: center;\n  gap: 1rem;\n}\n\nbutton {\n  padding: 0.5rem 1.5rem;\n  font-size: 1.25rem;\n  border: none;\n  border-radius: 4px;\n  cursor: pointer;\n  background-color: #4a90e2;\n  color: white;\n  transition: background-color 0.2s;\n}\n\nbutton:hover {\n  background-color: #357abd;\n}\n\nbutton:nth-child(2) {\n  background-color: #e27c4a;\n}\n\nbutton:nth-child(2):hover {\n  background-color: #c96a3b;\n}"
                }
              }
            }
            
            The model must adhere to this; no other response format is allowed. The model's response must include
            both the 'chat' and 'prototype' keys at the top-level and only those. 

            ### File Formats
            The model must use the .jsx extension for JavaScript files.

            ### What the code the model generates must be like
            1. Pages the model generates must use <div class="page">. Only one of those must have class="page active".
            2. The model always ensures modularity and reusability where possible.
            3. The model always implements responsive user interface design.
            5. The model always ensures consistent styling throughout.
            6. The model's code is always clean, self-documenting code (the model never adds comments anywhere). 
            7. The model should attempt to implement proper error handling, if relevant.
            8. The model should attempt to include input validation, if relevant.
            10. The model must add event listeners for user interactions.
            11. The model always implements immediate feedback mechanisms.
            12. The model always uses dummy data for immediate experimentation.
            
            ### Technologies the model can use
            Choose appropriate technologies from:
            1. Frontend: HTML5, CSS3, JavaScript (ES6+)
            2. Frameworks: React, Vue, Angular, Svelte.
            3. Styling: Tailwind, Bootstrap, Material-UI.
            4. Backend: Node.js.
            5. Backend Frameworks: Express, Spring.
            6. Building: Vite
            
            ### What the model must do
            Generate production-quality code based on:
            1. The user's prompt.
            2. The provided functional requirements (provided).
            3. Available reference templates (provided).
            """.trimIndent()
        val userMessage =
            """
            I want you to generate a lightweight proof-of-concept prototype for a system with the following 
            high-level specification: "$userPrompt"
            """.trimIndent()

        val functionalRequirementsMessage =
            """
            The model always considers the following functional requirements in addition to the user's message:
            $requirements
            """.trimIndent()

        val templatesMessage =
            """
             The model always considers the following reference templates. The model uses them to help generate a response. The model NEVER simply describes them):
            ${templates.joinToString(separator = "\n\n")}
            """.trimIndent()

        val prevCodeMessage =
            """
            The model must consider the current code when generating new code.
            If the provided current code is related and useful, the model must base the new code on the current code, 
            incorporating and extending existing features.
            This is the current code:
            $previousGeneration
            """.trimIndent()

        val finalPromptMessage =
            """
            Now the model will shortly produce the final JSON strictly following the schema and example provided.
            The response must include both the 'chat' and 'prototype' keys at the top-level and only those.
            The 'chat' key must have a string value, representing a short message describing what the model has done.
            The 'prototype' key must have an object value, containing the prototype structure the model has generated.
            
            The model always incorporates each reference template provided into its respective file in the prototype.files object.
            The model must never use back-ticked strings; the model must convert those to regular strings. For instance, `EXAMPLE` is not allowed, but EXAMPLE is. 
            The model never ignores the reference templates, rather it extends/modifies/combines them to fit the functional requirements. 
            The model should adjust the code from the templates to ensure they compile and run in the WebContainer environment. 
            The model always adds dependencies in package.json for React, ReactDOM, Vite, and anything else needed.
            
            The final code must run `npm install` and `npm start` without errors in a WebContainer. 
            The model always includes the script definition/declaration as files in its response.
             
            Now the model must produce a response.
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
                // Previous code message
                if (previousGeneration != null) {
                    println("PREVIOUS CODE MESSAGE IS NOT NULL IN PROMPTING TOOLS")
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put("content", prevCodeMessage)
                        },
                    )
                }
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
    fun formatResponseJson(response: String): String = cleanLlmResponse(response).also { println(it) }

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

        return response.substring(openingBrace, closingBrace).trim()
    }
}
