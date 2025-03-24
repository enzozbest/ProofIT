package prototype.helpers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import prototype.FileContent
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileContentTest {
    @Test
    fun `Test FileContent Class`() {
        val response = FileContent("content")
        assertEquals("content", response.content)
    }

    @Test
    fun `Test FileContent serialises to JSON`() {
        val response = FileContent("content")
        val json = Json.encodeToString(response)
        val expected = """{"content":"content"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `Test FileContent de-serialises from JSON`() {
        val expected = FileContent("content")
        val response =
            """
            {
            "content":"content"
            }
            """.trimIndent()
        val json = Json.decodeFromString<FileContent>(response)
        assertEquals(expected, json)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test FileContent deserialization missing a field`() {
        val json = """{}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<FileContent>(json)
        }
    }
}
