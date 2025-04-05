package prototype.helpers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LLMOptionsTest {
    @Test
    fun `Test OpenAIOptions with non-default values`() {
        val test =
            OpenAIOptions(
                temperature = 0.7,
                max_tokens = 150,
                top_p = 0.9,
                frequency_penalty = 0.5,
                presence_penalty = 0.5,
                stop = listOf("END"),
            )

        assertEquals(0.7, test.temperature)
        assertEquals(150, test.max_tokens)
        assertEquals(0.9, test.top_p)
        assertEquals(0.5, test.frequency_penalty)
        assertEquals(0.5, test.presence_penalty)
        assertEquals(listOf("END"), test.stop)
    }

    @Test
    fun `Test OpenAIOptions with default temperature`() {
        val test =
            OpenAIOptions(
                max_tokens = 150,
                top_p = 0.9,
                frequency_penalty = 0.5,
                presence_penalty = 0.5,
                stop = listOf("END"),
            )

        assertEquals(0.40, test.temperature)
        assertEquals(150, test.max_tokens)
        assertEquals(0.9, test.top_p)
        assertEquals(0.5, test.frequency_penalty)
        assertEquals(0.5, test.presence_penalty)
        assertEquals(listOf("END"), test.stop)
    }
}
