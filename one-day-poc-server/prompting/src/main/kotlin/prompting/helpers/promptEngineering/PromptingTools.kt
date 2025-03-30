package prompting.helpers.promptEngineering

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PromptingTools {
    private val preamble =
        """
        The model is an expert software engineer specializing in creating high-quality, production-ready lightweight 
        proof-of-concept prototypes that run in WebContainers. The model must answer based on 
        the provided functional requirements and templates. The model's job is to extend/modify/combine the given 
        templates together to fit the functional requirements and create a full working solution.
        """.trimIndent()
    private val responseFormat =
        """
        ### Response Format
        The model always responds with a single valid JSON object only. The model never includes any explanations,
        comments, or additional text. 

        ### Response Structure
        The model's response must strictly obey the example provided. The model is
        not allowed to change this format in any way.
        """.trimIndent()

    private val example1 =
        """
        ### Example (your response must look like this):
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
        """.trimIndent()

    private val example2 =
        $$"""
        ### Example (your response must look like this):
        {
          "chat": "Here's a customer support chatbot prototype with a modern dark gradient UI. It includes React components for the chat interface with support for messages, typing indicators, and a message input box.",
          "prototype": {
              "files": {
                "package.json": {
                  "name": "ai-chat-app",
                  "version": "1.0.0",
                  "description": "AI Chat Application",
                  "main": "index.js",
                  "scripts": {
                    "start": "vite"
                  },
                  "dependencies": {
                    "react": "^18.2.0",
                    "react-dom": "^18.2.0",
                    "vite": "^4.0.0"
                  }
                },
                "index.html": "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\" />\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n  <title>Customer Support</title>\n  <link href=\"https://cdnjs.cloudflare.com/ajax/libs/tailwindcss/2.2.19/tailwind.min.css\" rel=\"stylesheet\">\n  <style>\n    body, html {\n      margin: 0;\n      padding: 0;\n      height: 100%;\n    }\n    body {\n      background: linear-gradient(135deg, #0f1e44 0%, #1e1a45 50%, #4a1a6c 100%);\n      color: white;\n      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;\n      height: 100vh;\n      display: flex;\n      justify-content: center;\n      align-items: center;\n    }\n    .chat-container {\n      background-color: rgba(30, 41, 75, 0.7);\n      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);\n      border: 1px solid rgba(255, 255, 255, 0.1);\n      backdrop-filter: blur(8px);\n    }\n    .no-scrollbar::-webkit-scrollbar {\n      display: none;\n    }\n    .no-scrollbar {\n      -ms-overflow-style: none;\n      scrollbar-width: none;\n    }\n    .message {\n      max-width: 80%;\n      margin-bottom: 1rem;\n    }\n    .user-message {\n      background-color: rgba(100, 116, 240, 0.3);\n      color: white;\n      border: 1px solid rgba(100, 116, 240, 0.5);\n    }\n    .assistant-message {\n      background-color: rgba(255, 255, 255, 0.1);\n      color: white;\n      border: 1px solid rgba(255, 255, 255, 0.15);\n    }\n    .chat-input {\n      background-color: rgba(30, 41, 75, 0.9);\n      border-top: 1px solid rgba(255, 255, 255, 0.1);\n    }\n    .chat-input input {\n      background-color: rgba(10, 20, 40, 0.5);\n      color: white;\n      border: 1px solid rgba(255, 255, 255, 0.2);\n    }\n    .chat-input input:focus {\n      outline: none;\n      border-color: rgba(100, 116, 240, 0.5);\n      box-shadow: 0 0 0 2px rgba(100, 116, 240, 0.2);\n    }\n    .typing-indicator span {\n      background-color: rgba(255, 255, 255, 0.5);\n    }\n  </style>\n</head>\n<body>\n  <div id=\"root\"></div>\n  <script type=\"module\" src=\"/src/main.jsx\"></script>\n</body>\n</html>",
                "src/main.jsx": "import React from 'react';\nimport ReactDOM from 'react-dom';\nimport App from './App';\n\nReactDOM.render(<App />, document.getElementById('root'));",
                "src/App.jsx": "import React from 'react';\nimport ChatHeader from './components/ChatHeader';\nimport ChatContainer from './components/ChatContainer';\n\nconst App = () => {\n  return (\n    <div className=\"flex flex-col w-full max-w-3xl mx-auto h-screen md:h-5/6 max-h-screen\">\n      <ChatHeader />\n      <ChatContainer />\n    </div>\n  );\n};\n\nexport default App;",
                "src/components/ChatHeader.jsx": "import React from 'react';\n\nconst ChatHeader = () => {\n  return (\n    <div className=\"text-center py-8\">\n      <h1 className=\"text-4xl font-medium mb-2 text-white\">How can we help?</h1>\n      <p className=\"text-gray-300\">\n        Our AI assistant is here to answer your questions and provide support 24/7.\n      </p>\n    </div>\n  );\n};\n\nexport default ChatHeader;",
                "src/components/ChatContainer.jsx": "import React, { useState, useRef, useEffect } from 'react';\nimport ChatMessage from './ChatMessage';\nimport ChatFooter from './ChatFooter';\nimport MessageInput from './MessageInput';\nimport CustomerSupportHeader from './CustomerSupportHeader';\n\nconst ChatContainer = () => {\n  const [messages, setMessages] = useState([\n    {\n      content: \"Hi there! I'm your virtual assistant. How can I help you today?\",\n      sender: 'assistant',\n      timestamp: '19:53'\n    }\n  ]);\n  const [isTyping, setIsTyping] = useState(false);\n  const messagesEndRef = useRef(null);\n\n  useEffect(() => {\n    scrollToBottom();\n  }, [messages]);\n\n  const scrollToBottom = () => {\n    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });\n  };\n\n  const handleSendMessage = (content) => {\n    if (!content.trim()) return;\n    \n    // Add user message\n    setMessages(prev => [\n      ...prev,\n      {\n        content,\n        sender: 'user',\n        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })\n      }\n    ]);\n    \n    // Show typing indicator\n    setIsTyping(true);\n    \n    // Simulate AI response\n    setTimeout(() => {\n      setIsTyping(false);\n      setMessages(prev => [\n        ...prev,\n        {\n          content: \"Thank you for your message. Our AI assistant is processing your request. How else can I help you?\",\n          sender: 'assistant',\n          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })\n        }\n      ]);\n    }, 1500);\n  };\n\n  return (\n    <div className=\"chat-container flex flex-col w-full rounded-xl overflow-hidden flex-grow\">\n      <CustomerSupportHeader />\n      \n      <div className=\"flex-grow overflow-y-auto p-4 no-scrollbar bg-opacity-20 bg-black\">\n        {messages.map((message, index) => (\n          <ChatMessage\n            key={index}\n            content={message.content}\n            sender={message.sender}\n            timestamp={message.timestamp}\n          />\n        ))}\n        \n        {isTyping && (\n          <div className=\"flex items-center space-x-2 p-3 max-w-xs rounded-lg assistant-message animate-pulse\">\n            <div className=\"typing-indicator flex space-x-1\">\n              <span className=\"w-2 h-2 rounded-full\"></span>\n              <span className=\"w-2 h-2 rounded-full\"></span>\n              <span className=\"w-2 h-2 rounded-full\"></span>\n            </div>\n          </div>\n        )}\n        \n        <div ref={messagesEndRef} />\n      </div>\n      \n      <MessageInput onSendMessage={handleSendMessage} />\n      <ChatFooter />\n    </div>\n  );\n};\n\nexport default ChatContainer;",
                "src/components/CustomerSupportHeader.jsx": "import React from 'react';\n\nconst CustomerSupportHeader = () => {\n  return (\n    <div className=\"flex items-center justify-between p-4 bg-opacity-30 bg-black border-b border-opacity-20 border-white\">\n      <div className=\"flex items-center space-x-3\">\n        <div className=\"bg-blue-400 bg-opacity-20 text-blue-300 p-3 rounded-full\">\n          <svg width=\"20\" height=\"20\" viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">\n            <path d=\"M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM12 5C13.66 5 15 6.34 15 8C15 9.66 13.66 11 12 11C10.34 11 9 9.66 9 8C9 6.34 10.34 5 12 5ZM12 19.2C9.5 19.2 7.29 17.92 6 15.98C6.03 13.99 10 12.9 12 12.9C13.99 12.9 17.97 13.99 18 15.98C16.71 17.92 14.5 19.2 12 19.2Z\" fill=\"currentColor\"/>\n          </svg>\n        </div>\n        <div>\n          <div className=\"font-semibold\">Customer Support</div>\n          <div className=\"flex items-center text-xs text-green-300\">\n            <div className=\"w-2 h-2 bg-green-400 rounded-full mr-1\"></div>\n            Online\n          </div>\n        </div>\n      </div>\n      <div className=\"bg-opacity-20 bg-white text-gray-100 px-4 py-1 rounded-full text-sm\">\n        Self Service AI\n      </div>\n    </div>\n  );\n};\n\nexport default CustomerSupportHeader;",
                "src/components/ChatMessage.jsx": "import React from 'react';\n\nconst ChatMessage = ({ content, sender, timestamp }) => {\n  const isAssistant = sender === 'assistant';\n  \n  return (\n    <div className={`flex ${isAssistant ? 'justify-start' : 'justify-end'} mb-4`}>\n      <div className={`message p-3 rounded-xl ${isAssistant ? 'assistant-message' : 'user-message'}`}>\n        <div className=\"break-words\">{content}</div>\n        <div className={`text-xs mt-1 ${isAssistant ? 'text-gray-300' : 'text-blue-200'}`}>\n          {timestamp}\n        </div>\n      </div>\n    </div>\n  );\n};\n\nexport default ChatMessage;",
                "src/components/MessageInput.jsx": "import React, { useState } from 'react';\n\nconst MessageInput = ({ onSendMessage }) => {\n  const [message, setMessage] = useState('');\n\n  const handleSubmit = (e) => {\n    e.preventDefault();\n    if (message.trim()) {\n      onSendMessage(message);\n      setMessage('');\n    }\n  };\n\n  return (\n    <form onSubmit={handleSubmit} className=\"chat-input p-4\">\n      <div className=\"relative flex items-center\">\n        <input\n          type=\"text\"\n          placeholder=\"Ask me anything...\"\n          value={message}\n          onChange={(e) => setMessage(e.target.value)}\n          className=\"w-full p-3 pr-10 rounded-full focus:outline-none\"\n        />\n        <button \n          type=\"submit\"\n          className=\"absolute right-3 p-1 rounded-full text-blue-300 hover:text-blue-100\"\n          disabled={!message.trim()}\n        >\n          <svg xmlns=\"http://www.w3.org/2000/svg\" className=\"h-5 w-5\" fill=\"none\" viewBox=\"0 0 24 24\" stroke=\"currentColor\">\n            <path strokeLinecap=\"round\" strokeLinejoin=\"round\" strokeWidth={2} d=\"M14 5l7 7m0 0l-7 7m7-7H3\" />\n          </svg>\n        </button>\n      </div>\n    </form>\n  );\n};\n\nexport default MessageInput;",
                "src/components/ChatFooter.jsx": "import React from 'react';\n\nconst ChatFooter = () => {\n  return (\n    <div className=\"text-center text-xs text-gray-400 py-2 bg-black bg-opacity-30 border-t border-opacity-10 border-white\">\n      Powered by AI • Responses may not be accurate\n    </div>\n  );\n};\n\nexport default ChatFooter;"
              }
            }
        }
        """.trimIndent()

    private val codeRules =
        """
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
        2. Frameworks: React (18+).
        3. Styling: Tailwind, Bootstrap, Material-UI.
        4. Backend: Node.js.
        5. Backend Frameworks: Express, Spring.
        6. Building: Vite
        """.trimIndent()

    private val task =
        """
        ### What the model must do
        Generate production-quality code based on:
        1. The user's prompt.
        2. The provided functional requirements (provided).
        3. Available reference templates (provided).
        """.trimIndent()

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
    fun ollamaPrompt(
        userPrompt: String,
        requirements: String,
        templates: List<String>,
        previousGeneration: String? = null,
    ): String {
        val systemMessage = ollamaSystemMessage
        val userMessage = getUserMessage(userPrompt)
        val functionalRequirementsMessage = getFunctionalReqsMessage(requirements)
        val templatesMessage = getTemplatesMessage(templates)
        val prevCodeMessage = getPreviousGenerationMessage(previousGeneration)
        val finalPromptMessage =
            """
            Now the model will shortly produce the final JSON strictly following the format of the example provided.
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

    fun openAIPrompt(
        userPrompt: String,
        requirements: String,
        templates: List<String>,
        previousGeneration: String? = null,
    ): String {
        val systemMessage = openAISystemMessage
        val userMessage = getUserMessage(userPrompt)
        val functionalRequirementsMessage = getFunctionalReqsMessage(requirements)
        val templatesMessage = getTemplatesMessage(templates)
        val prevCodeMessage = getPreviousGenerationMessage(previousGeneration)

        val messagesArray =
            buildJsonArray {
                // Main system constraints
                add(
                    buildJsonObject {
                        put("role", "developer")
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
                        put("role", "developer")
                        put("content", functionalRequirementsMessage)
                    },
                )
                // Previous code message
                if (previousGeneration != null) {
                    add(
                        buildJsonObject {
                            put("role", "developer")
                            put("content", prevCodeMessage)
                        },
                    )
                }
                // Additional system-level templates
                add(
                    buildJsonObject {
                        put("role", "developer")
                        put("content", templatesMessage)
                    },
                )
                // Final "user" request: produce the final JSON response
                add(
                    buildJsonObject {
                        put("role", "developer")
                        put("content", "Now, the model must produce the final JSON response.")
                    },
                )
            }
        return Json.encodeToString(JsonArray.serializer(), messagesArray)
    }

    private val openAISystemMessage =
        """
        $preamble

        $responseFormat
        
        $example1
        
        $example2

        $codeRules
        
        $task
        """.trimIndent()

    private val ollamaSystemMessage =
        """
        $preamble

        $responseFormat
        
        $example1

        The model must adhere to this; no other response format is allowed. The model's response must include
        both the 'chat' and 'prototype' keys at the top-level and only those. 

        ### File Formats
        The model must use the .jsx extension for JavaScript files.

        $codeRules
        
        $task
        """.trimIndent()

    private fun getUserMessage(userPrompt: String) =
        """
        I want you to generate a lightweight proof-of-concept prototype for a system with the following 
        high-level specification: "$userPrompt"
        """.trimIndent()

    private fun getFunctionalReqsMessage(requirements: String) =
        """
        The model will now receive the functional requirements the model must consider in addition to the user's prompt:
        $requirements
        """.trimIndent()

    private fun getTemplatesMessage(templates: List<String>) =
        if (templates.isNotEmpty()) {
            """
            The model will now receive the templates the model must consider in addition to the user's prompt:
            ${templates.joinToString(", ")}
            """.trimIndent()
        } else {
            """
            The model will not receive any templates for this generation.
            """.trimIndent()
        }

    private fun getPreviousGenerationMessage(previousGeneration: String?): String =
        if (previousGeneration != null) {
            """
            The model will now receive its previous response for an attempt at generating the prototype for this prompt.
            The user is not happy with the result. Thus, you must consider what you did previously and the user's requested
            refinements. The refinements requested can be found in the user's prompt.
            $previousGeneration
            """.trimIndent()
        } else {
            """
            This is the first attempt at generating a prototype for this prompt.
            """.trimIndent()
        }

    /**
     * Formats the response from the LLM to remove new line characters and ensure it is valid JSON.
     * If the JSON is invalid initially, it will attempt to clean the response and try again.
     *
     * @param response The raw response from the LLM as a string
     * @return The formatted JSON response as a JsonObject
     */
    fun formatResponseJson(response: String): String = cleanLlmResponse(response)

    /**
     * Extracts and cleans a JSON object from an LLM response string.
     *
     * Identifies the first opening '{' and last closing '}' brace to extract the JSON object,]
     * then removes comments, handles escaped quotations, and normalizes whitespace.
     *
     * @param response The raw string from an LLM that may contain a JSON object
     * @return A cleaned string containing only the JSON object ready for parsing
     */
    fun cleanLlmResponse(response: String): String {
        val openingBrace = response.indexOf('{')
        val closingBrace =
            response.length -
                response
                    .reversed()
                    .indexOf('}') // As a "forwards" index pointing to the character just after the last '}'

        return response.substring(openingBrace, closingBrace).trim()
    }
}
