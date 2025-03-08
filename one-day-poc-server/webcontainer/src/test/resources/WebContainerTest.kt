package kcl.seg.rtt.webcontainer

import kcl.seg.rtt.prototype.FileContent
import kcl.seg.rtt.prototype.LlmResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*

class WebContainerTest {
    
    @BeforeEach
    fun setup() {
        // Use an empty response instead of null
        val emptyResponse = LlmResponse(
            mainFile = "",
            files = emptyMap()
        )
        WebContainerState.updateResponse(emptyResponse)
    }
    
    @Test
    @DisplayName("WebContainerState should store and retrieve LlmResponse correctly")
    fun testWebContainerStateStorage() {
        // Create a test response
        val testResponse = LlmResponse(
            mainFile = "index.html",
            files = mapOf(
                "html" to FileContent("<html><body>Test</body></html>"),
                "css" to FileContent("body { color: blue; }"),
                "js" to FileContent("console.log('Hello');")
            )
        )
        
        // Store the response
        WebContainerState.updateResponse(testResponse)
        
        // Retrieve and verify
        val retrievedResponse = WebContainerState.getLatestResponse()
        assertNotNull(retrievedResponse, "Retrieved response should not be null")
        assertEquals("index.html", retrievedResponse?.mainFile, "Main file should match")
        assertEquals(3, retrievedResponse?.files?.size, "Should have 3 files")
        assertTrue(retrievedResponse?.files?.containsKey("html") == true, "Should contain HTML file")
        assertEquals(
            "<html><body>Test</body></html>",
            retrievedResponse?.files?.get("html")?.content,
            "HTML content should match"
        )
    }
}