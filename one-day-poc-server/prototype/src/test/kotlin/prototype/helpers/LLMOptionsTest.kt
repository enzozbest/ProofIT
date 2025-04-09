package prototype.helpers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LLMOptionsTest {
    @Test
    fun `Test OpenAIOptions with non-default values`() {
        val test =
            OpenAIOptions(
                temperature = 0.7,
                topP = 0.9,
            )

        assertEquals(0.7, test.temperature)
        assertEquals(0.9, test.topP)
    }

    @Test
    fun `Test OpenAIOptions with default temperature`() {
        val test =
            OpenAIOptions(
                topP = 0.9,
            )

        assertEquals(0.40, test.temperature)
        assertEquals(0.9, test.topP)
    }
}
