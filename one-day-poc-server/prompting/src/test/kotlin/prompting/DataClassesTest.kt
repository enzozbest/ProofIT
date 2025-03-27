package prompting

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class DataClassesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }

    @Test
    fun `test ChatResponse serialization and deserialization`() {
        val timestamp = Instant.now().toString()
        val original =
            ChatResponse(
                message = "Test message",
                role = "Test role",
                timestamp = timestamp,
                messageId = "123",
            )

        val jsonString = json.encodeToString(original)

        val deserialized = json.decodeFromString<ChatResponse>(jsonString)

        assertEquals(original.message, deserialized.message)
        assertEquals(original.role, deserialized.role)
        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.messageId, deserialized.messageId)
    }

    @Test
    fun `test ChatResponse with default role`() {
        val timestamp = Instant.now().toString()
        val chatResponse =
            ChatResponse(
                message = "Test message",
                timestamp = timestamp,
                messageId = "123",
            )

        assertEquals("LLM", chatResponse.role)

        val jsonString = json.encodeToString(chatResponse)
        val deserialized = json.decodeFromString<ChatResponse>(jsonString)

        assertEquals("LLM", deserialized.role)
    }
}
