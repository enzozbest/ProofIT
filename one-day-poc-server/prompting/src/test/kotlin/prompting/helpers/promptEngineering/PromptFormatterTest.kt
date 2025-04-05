package prompting.helpers.promptEngineering

import org.junit.jupiter.api.Test
import prompting.helpers.OllamaPromptFormatter
import prompting.helpers.OpenAIPromptFormatter
import prompting.helpers.PromptFormatterFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PromptFormatterTest {
    @Test
    fun `Test PromptFormatterFactory returns OpenAIPromptFormatter`() {
        val formatter = PromptFormatterFactory.getFormatter("openai")
        assert(formatter is OpenAIPromptFormatter)
    }

    @Test
    fun `Test PromptFormatterFactory returns OllamaPromptFormatter`() {
        val formatter = PromptFormatterFactory.getFormatter("local")
        assert(formatter is OllamaPromptFormatter)
    }

    @Test
    fun `Test PromptFormatterFactory throws exception with invalid route`() {
        val message =
            assertFailsWith<IllegalArgumentException> {
                PromptFormatterFactory.getFormatter("invalid route")
            }.message

        assertEquals("Invalid route invalid route", message)
    }

    @Test
    fun `Test format function of OpenAIPromptFormatter`() {
        val formatter = OpenAIPromptFormatter()
        val test = formatter.format("This is a test", "This is a test", listOf("This is a test"), "This is a test")
        val expected =
            PromptingTools.openAIPrompt("This is a test", "This is a test", listOf("This is a test"), "This is a test")

        assertEquals(expected, test)
    }
}
