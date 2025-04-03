package prototype.services

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import prototype.PredefinedPrototype
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for the PredefinedPrototype data class.
 */
class PredefinedPrototypeTest {

    @Test
    fun `test constructor and properties`() {
        // Create a test JsonObject
        val jsonObject = JsonObject(mapOf("key" to JsonPrimitive("value")))

        // Create an instance of PredefinedPrototype
        val prototype = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("test", "prototype"),
            files = jsonObject
        )

        // Verify properties
        assertEquals("Test message", prototype.message)
        assertEquals(listOf("test", "prototype"), prototype.keywords)
        assertEquals(jsonObject, prototype.files)
    }

    @Test
    fun `test equals and hashCode`() {
        // Create test JsonObjects
        val jsonObject1 = JsonObject(mapOf("key" to JsonPrimitive("value")))
        val jsonObject2 = JsonObject(mapOf("key" to JsonPrimitive("value")))
        val jsonObject3 = JsonObject(mapOf("different" to JsonPrimitive("value")))

        // Create instances with same values
        val prototype1 = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("test", "prototype"),
            files = jsonObject1
        )

        val prototype2 = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("test", "prototype"),
            files = jsonObject2
        )

        // Create instance with different values
        val prototype3 = PredefinedPrototype(
            message = "Different message",
            keywords = listOf("test", "prototype"),
            files = jsonObject1
        )

        val prototype4 = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("different", "keywords"),
            files = jsonObject1
        )

        val prototype5 = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("test", "prototype"),
            files = jsonObject3
        )

        // Test equals
        assertEquals(prototype1, prototype2)
        assertNotEquals(prototype1, prototype3)
        assertNotEquals(prototype1, prototype4)
        assertNotEquals(prototype1, prototype5)

        // Test hashCode
        assertEquals(prototype1.hashCode(), prototype2.hashCode())
    }

    @Test
    fun `test toString`() {
        val jsonObject = JsonObject(mapOf("key" to JsonPrimitive("value")))

        val prototype = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("test", "prototype"),
            files = jsonObject
        )

        val toStringResult = prototype.toString()

        // Verify toString contains all properties
        assertTrue(toStringResult.contains("message=Test message"))
        assertTrue(toStringResult.contains("keywords=[test, prototype]"))
        assertTrue(toStringResult.contains("files={\"key\":\"value\"}"))
    }

    @Test
    fun `test component functions for destructuring`() {
        val jsonObject = JsonObject(mapOf("key" to JsonPrimitive("value")))

        val prototype = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("test", "prototype"),
            files = jsonObject
        )

        // Test destructuring
        val (message, keywords, files) = prototype

        assertEquals("Test message", message)
        assertEquals(listOf("test", "prototype"), keywords)
        assertEquals(jsonObject, files)
    }

    @Test
    fun `test copy function`() {
        val jsonObject = JsonObject(mapOf("key" to JsonPrimitive("value")))

        val prototype = PredefinedPrototype(
            message = "Test message",
            keywords = listOf("test", "prototype"),
            files = jsonObject
        )

        // Test copy with no changes
        val copiedPrototype = prototype.copy()
        assertEquals(prototype, copiedPrototype)

        // Test copy with changes to message
        val copiedWithMessageChange = prototype.copy(message = "New message")
        assertEquals("New message", copiedWithMessageChange.message)
        assertEquals(prototype.keywords, copiedWithMessageChange.keywords)
        assertEquals(prototype.files, copiedWithMessageChange.files)

        // Test copy with changes to keywords
        val copiedWithKeywordsChange = prototype.copy(keywords = listOf("new", "keywords"))
        assertEquals(prototype.message, copiedWithKeywordsChange.message)
        assertEquals(listOf("new", "keywords"), copiedWithKeywordsChange.keywords)
        assertEquals(prototype.files, copiedWithKeywordsChange.files)

        // Test copy with changes to files
        val newJsonObject = JsonObject(mapOf("new" to JsonPrimitive("value")))
        val copiedWithFilesChange = prototype.copy(files = newJsonObject)
        assertEquals(prototype.message, copiedWithFilesChange.message)
        assertEquals(prototype.keywords, copiedWithFilesChange.keywords)
        assertEquals(newJsonObject, copiedWithFilesChange.files)
    }
}
