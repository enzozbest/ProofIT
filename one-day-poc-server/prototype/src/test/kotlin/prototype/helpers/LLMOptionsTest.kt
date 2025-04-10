package prototype.helpers

import kotlinx.serialization.json.Json
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

    @Test
    fun `Test serialization and deserialization of OpenAIOptions`() {
        val original = OpenAIOptions(temperature = 0.5, topP = 0.8)
        val jsonString = Json.encodeToString(OpenAIOptions.serializer(), original)
        val deserialized = Json.decodeFromString(OpenAIOptions.serializer(), jsonString)
        assertEquals(original, deserialized)
    }

    @Test
    fun `Test deserialization missing fields`() {
        val jsonString = """{"top_p": 0.8}"""
        val deserialized = Json.decodeFromString(OpenAIOptions.serializer(), jsonString)
        assertEquals(0.40, deserialized.temperature)
        assertEquals(0.8, deserialized.topP)
    }
}
