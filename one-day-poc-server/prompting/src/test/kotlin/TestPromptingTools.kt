package kcl.seg.rtt.prompting.helpers

import kcl.seg.rtt.prompting.helpers.promptEngineering.PromptingTools
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.*
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
        assertTrue(result is JsonObject)
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
        assertTrue(result is JsonObject)
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
        assertTrue(result is JsonObject)
    }

    @Test
    fun `test formatResponseJson with completely invalid JSON throws exception`() {
        val input = "This is not JSON at all"
        assertThrows<Exception> {
            promptingTools.formatResponseJson(input)
        }
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
        assertTrue(result is JsonObject)
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

        keywords.forEach { keyword ->
            assertTrue(result.contains(keyword))
        }
        assertTrue(result.contains(prompt))
        assertTrue(result.contains("### Response Format"))
        assertTrue(result.contains("### Requirements Guidelines"))
        assertTrue(result.contains("### JSON Structure Example"))
        assertTrue(result.contains("### Your Task"))
        assertTrue(result.contains("Generate comprehensive requirements based on:"))
    }

    @Test
    fun `test functionalRequirementsPrompt with empty keywords list`() {
        val prompt = "Create a login form"
        val keywords = emptyList<String>()

        val result = promptingTools.functionalRequirementsPrompt(prompt, keywords)

        assertTrue(result.contains(prompt))
        assertTrue(result.contains("### Response Format"))
        assertTrue(result.contains("### Requirements Guidelines"))
        assertTrue(result.contains("### JSON Structure Example"))
        assertTrue(result.contains("### Your Task"))
        assertTrue(result.contains("Generate comprehensive requirements based on:"))
        assertTrue(result.contains("**Keywords:**"))
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

        assertTrue(result.contains(userPrompt))
        assertTrue(result.contains(requirements))
        assertTrue(result.contains(templates))
        assertTrue(result.contains("### Response Format"))
        assertTrue(result.contains("### Technology Stack"))
        assertTrue(result.contains("### JSON Structure"))
        assertTrue(result.contains("### Your Task"))
        assertTrue(result.contains("Generate production-quality code"))
        assertTrue(result.contains("User requirements"))
        assertTrue(result.contains("Provided functional requirements"))
        assertTrue(result.contains("Available templates"))
        assertTrue(result.contains("Modern development best practices"))
        assertTrue(result.contains("\"html\""))
        assertTrue(result.contains("\"css\""))
        assertTrue(result.contains("\"frameworks\""))
        assertTrue(result.contains("\"dependencies\""))
        assertTrue(result.contains("React"))
        assertTrue(result.contains("Tailwind"))
        assertTrue(result.contains("1. User requirements"))
        assertTrue(result.contains("2. Provided functional requirements"))
        assertTrue(result.contains("3. Available templates"))
        assertTrue(result.contains("4. Modern development best practices"))
    }
}
