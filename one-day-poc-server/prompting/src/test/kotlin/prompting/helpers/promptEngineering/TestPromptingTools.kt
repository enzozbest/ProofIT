package prompting.helpers.promptEngineering

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
    fun `test formatResponseJson with valid JSON`() {
        val input = """{"key": "value", "nested": {"inner": 42}}"""
        val result = promptingTools.formatResponseJson(input)
        assertNotNull(result)
        assertTrue(result.contains("\"key\""))
        assertTrue(result.contains("\"value\""))
        assertTrue(result.contains("\"nested\""))
        assertTrue(result.contains("\"inner\""))
        assertTrue(result.contains("42"))
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
        assertTrue(result.contains("\"key\""))
        assertTrue(result.contains("\"value\""))
        assertTrue(result.contains("\"nested\""))
        assertTrue(result.contains("\"inner\""))
        assertTrue(result.contains("\"data\""))
        assertTrue(result.contains("\"array\""))
        assertTrue(result.contains("this is a"))
        assertTrue(result.contains("multi-line string"))
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
        val result = promptingTools.formatResponseJson(input)
        assertTrue(result.contains("key"))
        assertTrue(result.contains("value"))
        assertTrue(result.contains("broken"))
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
        assertTrue(result.contains("\"key\""))
        assertTrue(result.contains("\"value\""))
        assertTrue(result.contains("\"nested\""))
        assertTrue(result.contains("\"inner\""))
        assertTrue(result.contains("\"data\""))
        assertTrue(result.contains("\"array\""))

        assertFalse(result.contains("// This is a header comment"))
        assertFalse(result.contains("/* trailing"))
    }

    @Test
    fun `test functionalRequirementsPrompt with keywords`() {
        val prompt = "Create a login form"
        val keywords = listOf("authentication", "security", "validation")

        val result = promptingTools.functionalRequirementsPrompt(prompt, keywords)

        keywords.forEach { keyword ->
            assertTrue(result.contains(keyword))
        }
        assertTrue(result.contains(prompt))

        assertTrue(result.contains("\"role\":\"system\""))
        assertTrue(result.contains("Response Format"))
        assertTrue(result.contains("Response Structure"))
        assertTrue(result.contains("What the model must do"))
        assertTrue(result.contains("Generate"))
    }

    @Test
    fun `test functionalRequirementsPrompt with empty keywords list`() {
        val prompt = "Create a login form"
        val keywords = emptyList<String>()

        val result = promptingTools.functionalRequirementsPrompt(prompt, keywords)

        assertTrue(result.contains(prompt))

        assertTrue(result.contains("\"role\":\"system\""))
        assertTrue(result.contains("Response Format"))
        assertTrue(result.contains("Response Structure"))
        assertTrue(result.contains("What the model must do"))
        assertTrue(result.contains("Generate"))

        assertTrue(result.contains("keywords"))
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

        assertTrue(result.contains(userPrompt), "Result should contain the user prompt somewhere in the string")

        assertTrue(result.contains("Must include email"), "Result should contain part of the requirements")
        assertTrue(result.contains("validation"), "Result should contain part of the requirements")
        assertTrue(result.contains("form"), "Result should contain part of the templates")
        assertTrue(result.contains("React"), "Result should contain part of the templates")

        assertTrue(result.contains("\"role\":\"system\""))
        assertTrue(result.contains("Response Format"))
        assertTrue(result.contains("Response Structure"))
        assertTrue(result.contains("What the model must do"))
        assertTrue(result.contains("Generate"))

        assertTrue(result.contains("requirements"))
        assertTrue(result.contains("templates"))

        assertTrue(result.contains("Technologies"))
    }
}
