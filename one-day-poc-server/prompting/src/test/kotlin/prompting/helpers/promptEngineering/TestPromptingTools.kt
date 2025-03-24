package prompting.helpers.promptEngineering

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestPromptingTools {
    companion object {
        private val promptingTools = PromptingTools
    }

    @Test
    fun `test removeComments removes C-style single line comments`() {
        val input =
            """
            code
            // this is a comment\n
            more code
            """.trimIndent()
        val expected =
            """
            code

            more code
            """.trimIndent()
        assertEquals(expected, promptingTools.run { input.removeComments() })
    }

    @Test
    fun `test removeComments removes C-style multi-line comments`() {
        val input =
            """
            code
            /* this is a
               multi-line comment */
            more code
            """.trimIndent()
        val expected =
            """
            code

            more code
            """.trimIndent()
        assertEquals(expected, promptingTools.run { input.removeComments() })
    }

    @Test
    fun `test removeComments removes Python-style comments`() {
        val input =
            """
            code
            # this is a comment\n
            more code
            """.trimIndent()
        val expected =
            """
            code

            more code
            """.trimIndent()
        assertEquals(expected, promptingTools.run { input.removeComments() })
    }

    @Test
    fun `test removeComments handles mixed comment types`() {
        val input =
            """
            code // C-style\n
            /* multi-line
               comment */
            more code # Python-style\n
            """.trimIndent()
        val expected =
            """
            code 

            more code 
            """.trimIndent()
        assertEquals(expected, promptingTools.run { input.removeComments() })
    }

    @Test
    fun `test removeComments with no comments returns original string`() {
        val input =
            """
            code
            more code
            final code
            """.trimIndent()
        assertEquals(input, promptingTools.run { input.removeComments() })
    }

    @Test
    fun `test formatResponseJson with valid JSON`() {
        val input = """{"key": "value", "nested": {"inner": 42}}"""
        val result = promptingTools.formatResponseJson(input)
        assertNotNull(result)
        assertEquals("value", result["key"]?.toString()?.trim('"'))
        assertTrue(result.containsKey("nested"))
        val nested = result["nested"]?.toString() ?: ""
        assertTrue(nested.contains("inner"))
        assertTrue(nested.contains("42"))
    }

    @Test
    fun `test formatResponseJson with JSON containing newlines`() {
        val input =
            """
            {
                "key": "value",
                "nested": {
                    "inner": "data",
                    "array": [1, 2, 3]
                },
                "multiline": "this is a
                             multi-line string"
            }
            """.trimIndent()
        val result = promptingTools.formatResponseJson(input)
        assertNotNull(result)
        assertEquals("value", result["key"]?.toString()?.trim('"'))

        val nested = result["nested"]?.toString() ?: ""
        assertTrue(nested.contains("inner"))
        assertTrue(nested.contains("data"))
        assertTrue(nested.contains("array"))
        assertTrue(nested.contains("[1,2,3]"))

        val multiline = result["multiline"]?.toString() ?: ""
        assertTrue(multiline.contains("this is a"))
        assertTrue(multiline.contains("multi-line string"))
    }

    @Test
    fun `test formatResponseJson with JSON needing cleaning`() {
        val input =
            """
            Some prefix text
            {
                "key": "value"
            }
            Some suffix text
            """.trimIndent()
        val result = promptingTools.formatResponseJson(input)
        assertNotNull(result)
    }

    @Test
    fun `test formatResponseJson with completely invalid JSON throws exception`() {
        val input = "This is not JSON at all"
        assertThrows<StringIndexOutOfBoundsException> {
            promptingTools.formatResponseJson(input)
        }
    }

    @Test
    fun `test formatResponseJson with malformed JSON throws exception`() {
        val input = "{ \"key\": \"value\", \"broken\": }"
        val exception = assertThrows<IllegalStateException> {
            promptingTools.formatResponseJson(input)
        }
        assertTrue(exception.message?.startsWith("ERROR:") ?: false)
    }

    @Test
    fun `test formatResponseJson with comments`() {
        val input =
            """
            // This is a header comment\n
            {
                "key": "value", // inline comment\n
                /* multi-line comment
                   explaining the nested structure */
                "nested": {
                    // nested comment\n
                    "inner": "data" /* with comment */,
                    # Python-style comment\n
                    "array": [1, 2, 3] // array comment\n
                }
            }
            /* trailing
               multi-line comment */
            """.trimIndent()

        val result = promptingTools.formatResponseJson(input)
        assertNotNull(result)
        assertEquals("value", result["key"]?.toString()?.trim('"'))

        val nested = result["nested"]?.toString() ?: ""
        assertTrue(nested.contains("inner"))
        assertTrue(nested.contains("data"))
        assertTrue(nested.contains("array"))
        assertTrue(nested.contains("[1,2,3]"))

        val resultString = result.toString()
        assertFalse(resultString.contains("//"))
        assertFalse(resultString.contains("/*"))
        assertFalse(resultString.contains("*/"))
        assertFalse(resultString.contains("#"))
    }

    @Test
    fun `test functionalRequirementsPrompt with keywords`() {
        val prompt = "Create a login form"
        val keywords = listOf("authentication", "security", "validation")

        val result = promptingTools.functionalRequirementsPrompt(prompt, keywords)

        // The result is now a JSON string, so we need to check that it contains the keywords and prompt
        keywords.forEach { keyword ->
            assertTrue(result.contains(keyword))
        }
        assertTrue(result.contains(prompt))

        // Check that the JSON string contains the system message with the expected content
        assertTrue(result.contains("\"role\":\"system\""))
        assertTrue(result.contains("Response Format"))
        assertTrue(result.contains("Requirements Rules"))
        assertTrue(result.contains("What the model must do"))
        assertTrue(result.contains("Generate comprehensive"))
    }

    @Test
    fun `test functionalRequirementsPrompt with empty keywords list`() {
        val prompt = "Create a login form"
        val keywords = emptyList<String>()

        val result = promptingTools.functionalRequirementsPrompt(prompt, keywords)

        // The result is now a JSON string, so we need to check that it contains the prompt
        assertTrue(result.contains(prompt))

        // Check that the JSON string contains the system message with the expected content
        assertTrue(result.contains("\"role\":\"system\""))
        assertTrue(result.contains("Response Format"))
        assertTrue(result.contains("Requirements Rules"))
        assertTrue(result.contains("What the model must do"))
        assertTrue(result.contains("Generate comprehensive"))

        // Check that the keywords message is included even with empty keywords
        assertTrue(result.contains("These are the keywords"))
    }

    @Test
    fun `test prototypePrompt generates correct prompt`() {
        val userPrompt = "Create a login form"
        val requirements =
            """
            1. Must include email and password fields
            2. Implement input validation
            3. Show error messages
            """.trimIndent()
        val templates =
            """
            {
                "html": {
                    "code": "<form>...</form>",
                    "frameworks": ["React"],
                    "dependencies": []
                },
                "css": {
                    "code": ".form { ... }",
                    "frameworks": ["Tailwind"],
                    "dependencies": []
                }
            }
            """.trimIndent()

        val result = promptingTools.prototypePrompt(userPrompt, requirements, listOf(templates))

        // Print the result for debugging
        println("[DEBUG_LOG] Result: $result")
        println("[DEBUG_LOG] User prompt: $userPrompt")

        // The userPrompt is embedded within a larger string in the JSON
        // It's included in the format: "content":"This is what I want you to generate a lightweight proof-of-concept prototype for:\n\"Create a login form\""
        val promptPattern = "generate a lightweight proof-of-concept prototype for:"
        assertTrue(result.contains(promptPattern), "Result should contain the prompt introduction")
        assertTrue(result.contains(userPrompt), "Result should contain the user prompt somewhere in the string")

        // Check for requirements and templates with more flexible assertions
        assertTrue(result.contains("Must include email"), "Result should contain part of the requirements")
        assertTrue(result.contains("validation"), "Result should contain part of the requirements")
        assertTrue(result.contains("form"), "Result should contain part of the templates")
        assertTrue(result.contains("React"), "Result should contain part of the templates")

        // Check that the JSON string contains the system message with the expected content
        assertTrue(result.contains("\"role\":\"system\""))
        assertTrue(result.contains("Response Format"))
        assertTrue(result.contains("Technology Stack"))
        assertTrue(result.contains("What the model must do"))
        assertTrue(result.contains("Generate production-quality code"))

        // Check that the templates and requirements are included
        assertTrue(result.contains("functional requirements"))
        assertTrue(result.contains("templates"))

        // Check that the technology stack information is included
        assertTrue(result.contains("html"))
        assertTrue(result.contains("css"))
        assertTrue(result.contains("frameworks"))
        assertTrue(result.contains("dependencies"))
    }
}
