package kcl.seg.rtt.chat

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ResponseTest {
    @Test
    fun `Test Response creation with valid parameters`() {
        val response = Response(
            time = "2025-01-01T12:00:00",
            message = "Test message"
        )

        assertEquals("2025-01-01T12:00:00", response.time)
        assertEquals("Test message", response.message)
    }

    @Test
    fun `Test Response serialization and deserialization`() {
        val response = Response(
            time = "2025-01-01T12:00:00",
            message = "Test message"
        )

        val jsonString = Json.encodeToString(Response.serializer(), response)
        val deserializedResponse = Json.decodeFromString(Response.serializer(), jsonString)

        assertEquals(response, deserializedResponse)
    }

    @Test
    fun `Test Response copy function`() {
        val originalResponse = Response(
            time = "2025-01-01T12:00:00",
            message = "Original message"
        )

        val copiedResponse = originalResponse.copy(message = "Updated message")

        assertEquals(originalResponse.time, copiedResponse.time)
        assertEquals("Updated message", copiedResponse.message)
    }

    @Test
    fun `Test Response deserialization fails with missing required fields`() {
        val invalidJson = """{"time":"2025-01-01T12:00:00"}"""

        assertThrows<SerializationException> {
            Json.decodeFromString<Response>(invalidJson)
        }
    }

    @Test
    fun `Test Response deserialization fails with invalid JSON`() {
        val invalidJson = """{"time":123,"message":"Test message"}"""

        assertThrows<SerializationException> {
            Json.decodeFromString<Response>(invalidJson)
        }
    }
}