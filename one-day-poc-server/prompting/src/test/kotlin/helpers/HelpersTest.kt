package helpers

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class HelpersTest {
    private val json = Json { ignoreUnknownKeys = false }

    @Test
    fun `test SanitisedPromptResult serialization and deserialization`() {
        val promptResult = SanitisedPromptResult(
            prompt = "test prompt",
            keywords = listOf("keyword1", "keyword2")
        )

        val jsonString = json.encodeToString(promptResult)
        val decoded = json.decodeFromString<SanitisedPromptResult>(jsonString)

        assertEquals(promptResult, decoded)
    }

    @Test
    fun `test SanitisedPromptResult deserialization with missing fields`() {
        val jsonString = """{"prompt": "test prompt"}"""

        assertThrows<SerializationException> {
            json.decodeFromString<SanitisedPromptResult>(jsonString)
        }
    }

    @Test
    fun `test SanitisedPromptResult with empty keywords list`() {
        val promptResult = SanitisedPromptResult(
            prompt = "test prompt",
            keywords = emptyList()
        )

        val jsonString = json.encodeToString(promptResult)
        val decoded = json.decodeFromString<SanitisedPromptResult>(jsonString)

        assertEquals(promptResult, decoded)
    }

    @Test
    fun `test Response serialization and deserialization`() {
        val response = Response(
            time = "2023-01-01",
            message = "test message"
        )

        val jsonString = json.encodeToString(response)
        val decoded = json.decodeFromString<Response>(jsonString)

        assertEquals(response, decoded)
    }

    @Test
    fun `test Response deserialization with missing fields`() {
        val jsonString = """{"time": "2023-01-01"}"""

        assertThrows<SerializationException> {
            json.decodeFromString<Response>(jsonString)
        }
    }

    @Test
    fun `test SanitisedPromptResult deserialization with unknown fields`() {
        val jsonString = """{"prompt": "test prompt", "keywords": ["test"], "unknown": "value"}"""

        assertThrows<SerializationException> {
            json.decodeFromString<SanitisedPromptResult>(jsonString)
        }
    }

    @Test
    fun `test Response deserialization with unknown fields`() {
        val jsonString = """{"time": "2023-01-01", "message": "test", "unknown": "value"}"""

        assertThrows<SerializationException> {
            json.decodeFromString<Response>(jsonString)
        }
    }

    @Test
    fun `test Response with empty strings`() {
        val response = Response(
            time = "",
            message = ""
        )

        val jsonString = json.encodeToString(response)
        val decoded = json.decodeFromString<Response>(jsonString)

        assertEquals(response, decoded)
    }
}
