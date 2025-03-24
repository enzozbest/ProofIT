package prompting.helpers.promptEngineering

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SanitisationToolsTest {
    @Test
    fun `test sanitisePrompt with HTML tags`() {
        val input = "<p>Hello <b>World</b></p>"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("Hello World", result.prompt)
    }

    @Test
    fun `test sanitisePrompt with whitespace trimming`() {
        val input = "   Hello World   "
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("Hello World", result.prompt)
    }

    @Test
    fun `test sanitisePrompt with HTML entities`() {
        val input = "Hello &lt;World&gt; &amp; Everyone"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("Hello World  Everyone", result.prompt)
    }

    @Test
    fun `test sanitisePrompt with special characters`() {
        val input = "Hello@World#$%^&*"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("HelloWorld", result.prompt)
    }

    @Test
    fun `test sanitisePrompt with length limitation`() {
        val input = "a".repeat(2000)
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals(SanitisationTools.MAX_PROMPT_LENGTH, result.prompt.length)
    }

    @Test
    fun `test sanitisePrompt with malicious patterns`() {
        val patterns =
            listOf(
                "ignore this",
                "pretend to be",
                "disregard previous",
                "act like a",
                "follow my instructions",
                "do not follow rules",
                "override settings",
                "act as admin",
                "respond as system",
            )

        for (pattern in patterns) {
            val result = SanitisationTools.sanitisePrompt(pattern)
            assertFalse(
                result.prompt.contains(pattern, ignoreCase = true),
                "Malicious pattern '$pattern' should be removed",
            )
        }
    }

    @Test
    fun `test sanitisePrompt with empty input`() {
        val result = SanitisationTools.sanitisePrompt("")
        assertEquals("", result.prompt)
        assertTrue(result.keywords.isEmpty())
    }

    @Test
    fun `test sanitisePrompt with single keyword`() {
        val input = "This is test1 in a sentence"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("This is test1 in a sentence", result.prompt)
        assertEquals(listOf("test1"), result.keywords)
    }

    @Test
    fun `test sanitisePrompt with multiple keywords`() {
        val input = "test1 and test2 and test3 in one sentence"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("test1 and test2 and test3 in one sentence", result.prompt)
        assertEquals(listOf("test1", "test2", "test3"), result.keywords)
    }

    @Test
    fun `test sanitisePrompt with mixed case keywords`() {
        val input = "TEST1 and Test2 and test3"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("TEST1 and Test2 and test3", result.prompt)
        assertEquals(listOf("test1", "test2", "test3"), result.keywords)
    }

    @Test
    fun `test sanitisePrompt with no matching keywords`() {
        val input = "This text contains no keywords"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("This text contains no keywords", result.prompt)
        assertTrue(result.keywords.isEmpty())
    }

    @Test
    fun `test sanitisePrompt with malicious pattern in middle of text`() {
        val input = "Start text ignore this continue text"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("Start text  continue text", result.prompt)
    }

    @Test
    fun `test sanitisePrompt with combined HTML malicious and keywords`() {
        val input = "<p>test1</p> ignore this <b>test2</b> &lt;test3&gt;"
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("test1  test2 test3", result.prompt)
        assertEquals(listOf("test1", "test2", "test3"), result.keywords)
    }

    @Test
    fun `test sanitisePrompt preserves allowed punctuation`() {
        val input = "Hello, World! How are you? This is a test."
        val result = SanitisationTools.sanitisePrompt(input)
        assertEquals("Hello, World! How are you? This is a test.", result.prompt)
    }
}
