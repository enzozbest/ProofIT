package prompting.helpers

import org.junit.jupiter.api.Test
import prototype.helpers.OllamaOptions
import prototype.helpers.OpenAIOptions
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OptionsFactoryTest {
    @Test
    fun `Test OptionsFactory returns OllamaOptions`() {
        val options = OptionsFactory.getOptions("local", 0.40)
        assert(options is OllamaOptions)
        assertEquals(0.40, (options as OllamaOptions).temperature)
    }

    @Test
    fun `Test OptionsFactory returns OpenAIOptions`() {
        val options = OptionsFactory.getOptions("openai", 0.40)
        assert(options is OpenAIOptions)
        assertEquals(0.40, (options as OpenAIOptions).temperature)
    }

    @Test
    fun `Test OptionsFactory with invalid route`() {
        val message =
            assertFailsWith<IllegalArgumentException> { OptionsFactory.getOptions("invalid route", 0.40) }.message

        assertEquals("Invalid route invalid route", message)
    }
}
