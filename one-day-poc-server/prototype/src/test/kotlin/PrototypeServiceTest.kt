import kcl.seg.rtt.prototype.FileContent
import kcl.seg.rtt.prototype.LlmResponse
import kcl.seg.rtt.prototype.OllamaService
import kcl.seg.rtt.prototype.PrototypeService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrototypeServiceTest {

    /**
     * This test covers the lines in generatePrototype:
     *
     *     val fullPrompt = createPrompt(prompt)
     *     return ollamaService.generateResponse(fullPrompt)
     *
     * Since createPrompt embeds the provided user prompt into the returned string (using "$userPrompt")
     * we verify that the full prompt contains both "User Request:" and the user prompt text.
     */
    @Test
    fun testGeneratePrototype_constructsPromptAndDelegatesResponse() = runBlocking {
        val ollamaServiceMock = mock<OllamaService>()
        val dummyResponse = LlmResponse(
            mainFile = "index.js",
            files = mapOf("index.js" to FileContent("dummy content"))
        )
        whenever(ollamaServiceMock.generateResponse(any())).thenReturn(Result.success(dummyResponse))

        val prototypeService = PrototypeService(ollamaServiceMock)
        val userPrompt = "Test prompt"


        val result = prototypeService.generatePrototype(userPrompt)

        assertEquals(Result.success(dummyResponse), result)

        val promptCaptor = argumentCaptor<String>()
        verify(ollamaServiceMock).generateResponse(promptCaptor.capture())
        val fullPrompt = promptCaptor.firstValue

        assertTrue(fullPrompt.contains("User Request:"), "Full prompt should contain 'User Request:'")
        assertTrue(fullPrompt.contains(userPrompt), "Full prompt should contain the user prompt text")
    }

    /**
     * This test covers the line in retrievePrototype that returns:
     *
     *     return "<html><body><h1>Hello from Prototype $id</h1></body></html>"
     */
    @Test
    fun testRetrievePrototype_returnsExpectedHtml() {
        // We can pass a dummy OllamaService since retrievePrototype does not use it.
        val dummyOllamaService = mock<OllamaService>()
        val prototypeService = PrototypeService(dummyOllamaService)
        val id = "123"
        val expectedHtml = "<html><body><h1>Hello from Prototype $id</h1></body></html>"
        val actualHtml = prototypeService.retrievePrototype(id)
        assertEquals(expectedHtml, actualHtml)
    }

    /**
     * This test covers the @Serializable annotation on the FileContent data class.
     * It verifies that an instance of FileContent is correctly serialized to JSON.
     */
    @Test
    fun testFileContentSerialization() {
        val fileContent = FileContent("<html>Test</html>")
        val jsonString = Json.encodeToString(fileContent)
        assertEquals("""{"content":"<html>Test</html>"}""", jsonString)
    }

    /**
     * Optionally, you can also test serialization of LlmResponse to ensure
     * that all @Serializable annotations in this file are covered.
     */
    @Test
    fun testLlmResponseSerialization() {
        val response = LlmResponse(
            mainFile = "index.html",
            files = mapOf("index.html" to FileContent("Sample content"))
        )
        val jsonString = Json.encodeToString(response)
        assertTrue(jsonString.contains("index.html"))
        assertTrue(jsonString.contains("Sample content"))
    }
}
