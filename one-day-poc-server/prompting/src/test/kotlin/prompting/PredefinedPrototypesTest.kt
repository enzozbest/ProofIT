package prompting

import io.mockk.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prototype.PredefinedPrototypeService
import prototype.PrototypeTemplate
import kotlin.test.assertEquals

class PredefinedPrototypesTest {
    @BeforeEach
    fun setUp() {
        mockkObject(PredefinedPrototypeService)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `run calls PredefinedPrototypeService and returns correct template`() {
        // Create a mock response from PredefinedPrototypeService
        val jsonObject =
            buildJsonObject {
                put("file1", "content1")
                put("file2", "content2")
            }
        val prototypeTemplate =
            PrototypeTemplate(
                chatMessage = "Test message",
                files = jsonObject,
            )

        // Mock the PredefinedPrototypeService.getPrototypeForPrompt method
        every { PredefinedPrototypeService.getPrototypeForPrompt(any()) } returns prototypeTemplate

        // Call the method under test
        val result = PredefinedPrototypes.run("test prompt")

        // Verify the result
        assertEquals("Test message", result.chatMessage)
        assertEquals(jsonObject.toString(), result.files)

        // Verify that PredefinedPrototypeService.getPrototypeForPrompt was called with the correct prompt
        verify { PredefinedPrototypeService.getPrototypeForPrompt("test prompt") }
    }

    @Test
    fun `run handles empty JsonObject correctly`() {
        // Create a mock response with an empty JsonObject
        val emptyJsonObject = JsonObject(emptyMap())
        val prototypeTemplate =
            PrototypeTemplate(
                chatMessage = "No files available",
                files = emptyJsonObject,
            )

        // Mock the PredefinedPrototypeService.getPrototypeForPrompt method
        every { PredefinedPrototypeService.getPrototypeForPrompt(any()) } returns prototypeTemplate

        // Call the method under test
        val result = PredefinedPrototypes.run("empty prompt")

        // Verify the result
        assertEquals("No files available", result.chatMessage)
        assertEquals(emptyJsonObject.toString(), result.files)

        // Verify that PredefinedPrototypeService.getPrototypeForPrompt was called with the correct prompt
        verify { PredefinedPrototypeService.getPrototypeForPrompt("empty prompt") }
    }
}
