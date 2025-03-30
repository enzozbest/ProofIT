package prototype

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import prototype.services.PredefinedPrototype
import prototype.services.PrototypeTemplate
import kotlin.test.assertEquals

class PredefinedPrototypeTest {
    @Test
    fun `PredefinedPrototype data class initialization and property access`() {
        // Create a JsonObject for the files property
        val files =
            buildJsonObject {
                put("file1.txt", "content1")
                put("file2.txt", "content2")
            }

        // Create a list of keywords
        val keywords = listOf("test", "example")

        // Create a PredefinedPrototype instance
        val prototype =
            PredefinedPrototype(
                message = "This is a test message",
                keywords = keywords,
                files = files,
            )

        // Verify the properties
        assertEquals("This is a test message", prototype.message)
        assertEquals(keywords, prototype.keywords)
        assertEquals(files, prototype.files)
    }

    @Test
    fun `PrototypeTemplate data class initialization and property access`() {
        // Create a JsonObject for the files property
        val files =
            buildJsonObject {
                put("file1.txt", "content1")
                put("file2.txt", "content2")
            }

        // Create a PrototypeTemplate instance
        val template =
            PrototypeTemplate(
                chatMessage = "This is a test message",
                files = files,
            )

        // Verify the properties
        assertEquals("This is a test message", template.chatMessage)
        assertEquals(files, template.files)
    }
}
