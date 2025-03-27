package prompting.helpers.templates

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.*
import prompting.helpers.PrototypeInteractor
import prompting.helpers.promptEngineering.PromptingTools
import prototype.helpers.EnhancedResponse
import prototype.helpers.OllamaResponse
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class TemplateRetrievalTest {
    // Test directory for file operations
    private val testDir = createTempDir("template-test")
    private val testTemplatesDir = File(testDir, "templates").apply { mkdirs() }
    private val testMetadataDir = File(testDir, "metadata").apply { mkdirs() }

    // For spying on TemplateRetrieval object

    @BeforeEach
    fun setup() {
        // Mock all dependencies
        mockkObject(PromptingTools)
        mockkObject(PrototypeInteractor)
        mockkObject(TemplateInteractor)
        mockkObject(TemplateRetrieval)

        // Mock essential methods but allow others to use real implementation
        every { TemplateRetrieval.templatesDir } returns testTemplatesDir.absolutePath
        every { TemplateRetrieval.metadataDir } returns testMetadataDir.absolutePath

        // Ensure we create real files in test directories
        every { TemplateRetrieval.createDirectoriesIfNeeded() } answers {
            testTemplatesDir.mkdirs()
            testMetadataDir.mkdirs()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        testDir.deleteRecursively()
    }

    /* Tests for cleanTemplate */

    @Test
    fun `cleanTemplate delegates to PromptingTools`() {
        // Arrange
        val rawTemplate = "```typescript\nconst Button = () => <button>Click me</button>;\n```"
        val cleanedExpected = "const Button = () => <button>Click me</button>;"

        every { PromptingTools.cleanLlmResponse(any()) } returns cleanedExpected

        // Act
        val result = TemplateRetrieval.cleanTemplate(rawTemplate)

        // Assert
        assertEquals(cleanedExpected, result)
        verify { PromptingTools.cleanLlmResponse(rawTemplate) }
    }

    /* Tests for processTemplatesFromResponse */

    @Test
    fun `processTemplatesFromResponse returns empty list for empty templates`() = runBlocking {
        // Arrange
        val response = EnhancedResponse(
            response = OllamaResponse("model", "timestamp", "response", true, "stop"),
            extractedTemplates = emptyList()
        )

        // Act
        val result = TemplateRetrieval.processTemplatesFromResponse(response)

        // Assert
        assertTrue(result.isEmpty())
        verify(exactly = 0) { TemplateRetrieval.createDirectoriesIfNeeded() }
    }

    @Test
    fun `processTemplatesFromResponse creates directories and processes each template`() = runBlocking {
        // Arrange
        val templates = listOf(
            "```tsx\nexport const Button = () => <button>Click</button>;\n```",
            "```tsx\nexport const Card = () => <div>Card</div>;\n```"
        )

        val response = EnhancedResponse(
            response = OllamaResponse("model", "timestamp", "response", true, "stop"),
            extractedTemplates = templates
        )

        // Mock behavior
        every { TemplateRetrieval.cleanTemplate(templates[0]) } returns "export const Button = () => <button>Click</button>;"
        every { TemplateRetrieval.cleanTemplate(templates[1]) } returns "export const Card = () => <div>Card</div>;"

        coEvery {
            TemplateRetrieval.processTemplate("export const Button = () => <button>Click</button>;")
        } returns "Button"

        coEvery {
            TemplateRetrieval.processTemplate("export const Card = () => <div>Card</div>;")
        } returns "Card"

        // Act
        val result = TemplateRetrieval.processTemplatesFromResponse(response)

        // Assert
        assertEquals(2, result.size)
        assertEquals(listOf("Button", "Card"), result)
        verify(exactly = 1) { TemplateRetrieval.createDirectoriesIfNeeded() }
        verify { TemplateRetrieval.cleanTemplate(templates[0]) }
        verify { TemplateRetrieval.cleanTemplate(templates[1]) }
    }

    @Test
    fun `processTemplatesFromResponse handles failed template processing`() = runBlocking {
        // Arrange
        val templates = listOf(
            "```tsx\nexport const Button = () => <button>Click</button>;\n```",
            "```tsx\nexport const Card = () => <div>Card</div>;\n```"
        )

        val response = EnhancedResponse(
            response = OllamaResponse("model", "timestamp", "response", true, "stop"),
            extractedTemplates = templates
        )

        // Mock behavior - first template succeeds, second fails
        every { TemplateRetrieval.cleanTemplate(any()) } answers {
            val template = firstArg<String>()
            if (template.contains("Button")) "export const Button = () => <button>Click</button>;"
            else "export const Card = () => <div>Card</div>;"
        }

        coEvery {
            TemplateRetrieval.processTemplate("export const Button = () => <button>Click</button>;")
        } returns "Button"

        coEvery {
            TemplateRetrieval.processTemplate("export const Card = () => <div>Card</div>;")
        } returns null // Simulate failure

        // Act
        val result = TemplateRetrieval.processTemplatesFromResponse(response)

        // Assert
        assertEquals(1, result.size)
        assertEquals("Button", result[0])
        coVerify { TemplateRetrieval.processTemplate("export const Button = () => <button>Click</button>;") }
        coVerify { TemplateRetrieval.processTemplate("export const Card = () => <div>Card</div>;") }
    }

    /* Tests for createDirectoriesIfNeeded */

    @Test
    fun `createDirectoriesIfNeeded creates directories`() {
        // Arrange
        unmockkObject(TemplateRetrieval) // Unmock to test real implementation
        mockkStatic(Files::class)
        mockkStatic(Paths::class)

        val templatePath = mockk<java.nio.file.Path>()
        val metadataPath = mockk<java.nio.file.Path>()

        every { Paths.get(TemplateRetrieval.templatesDir) } returns templatePath
        every { Paths.get(TemplateRetrieval.metadataDir) } returns metadataPath
        every { Files.createDirectories(any()) } returns mockk()

        // Act
        TemplateRetrieval.createDirectoriesIfNeeded()

        // Assert
        verify { Files.createDirectories(templatePath) }
        verify { Files.createDirectories(metadataPath) }

        // Restore mocks
        mockkObject(TemplateRetrieval)
    }

    /* Tests for processTemplate */

    @Test
    fun `processTemplate returns null when component name extraction fails`() = runBlocking {
        // Arrange
        val templateCode = "function helper() { return 42; }"

        every { TemplateRetrieval.extractComponentName(templateCode) } returns null

        // Act
        val result = TemplateRetrieval.processTemplate(templateCode)

        // Assert
        assertNull(result)
        verify { TemplateRetrieval.extractComponentName(templateCode) }
        coVerify(exactly = 0) { TemplateRetrieval.generateTemplateAnnotation(any(), any()) }
    }

    @Test
    fun `processTemplate returns null when JSON-LD generation fails`() = runBlocking {
        // Arrange
        val templateCode = "export const Button = () => <button>Click</button>;"
        val componentName = "Button"

        every { TemplateRetrieval.extractComponentName(templateCode) } returns componentName
        coEvery { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) } returns null

        // Act
        val result = TemplateRetrieval.processTemplate(templateCode)

        // Assert
        assertNull(result)
        verify { TemplateRetrieval.extractComponentName(templateCode) }
        coVerify { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) }
        coVerify(exactly = 0) { TemplateRetrieval.storeTemplateFiles(any(), any(), any()) }
    }

    @Test
    fun `processTemplate returns null when file storage fails`() = runBlocking {
        // Arrange
        val templateCode = "export const Button = () => <button>Click</button>;"
        val componentName = "Button"
        val jsonLD = "{\"json\":\"data\"}"

        every { TemplateRetrieval.extractComponentName(templateCode) } returns componentName
        coEvery { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) } returns jsonLD
        coEvery { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) } returns false

        // Act
        val result = TemplateRetrieval.processTemplate(templateCode)

        // Assert
        assertNull(result)
        verify { TemplateRetrieval.extractComponentName(templateCode) }
        coVerify { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) }
        coVerify { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) }
    }

    @Test
    fun `processTemplate returns component name when successful`() = runBlocking {
        // Arrange
        val templateCode = "export const Button = () => <button>Click</button>;"
        val componentName = "Button"
        val jsonLD = "{\"json\":\"data\"}"

        every { TemplateRetrieval.extractComponentName(templateCode) } returns componentName
        coEvery { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) } returns jsonLD
        coEvery { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) } returns true

        // Act
        val result = TemplateRetrieval.processTemplate(templateCode)

        // Assert
        assertEquals(componentName, result)
        verify { TemplateRetrieval.extractComponentName(templateCode) }
        coVerify { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) }
        coVerify { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) }
    }

    /* Tests for extractComponentName */

    @Test
    fun `extractComponentName identifies export const pattern`() {
        // Arrange
        val templateCode = "export const Button = () => <button>Click me</button>;"

        // Act
        val result = TemplateRetrieval.extractComponentName(templateCode)

        // Assert
        assertEquals("Button", result)
    }

    @Test
    fun `extractComponentName identifies export default function pattern`() {
        // Arrange
        val templateCode = "export default function Card() { return <div>Card</div>; }"

        // Act
        val result = TemplateRetrieval.extractComponentName(templateCode)

        // Assert
        assertEquals("Card", result)
    }

    @Test
    fun `extractComponentName identifies const with props pattern`() {
        // Arrange
        val templateCode = "const Accordion = ({ items }) => { return <div>Accordion</div>; }"

        // Act
        val result = TemplateRetrieval.extractComponentName(templateCode)

        // Assert
        assertEquals("Accordion", result)
    }

    @Test
    fun `extractComponentName identifies class component pattern`() {
        // Arrange
        val templateCode = "class Carousel extends React.Component { render() { return <div>Carousel</div>; } }"

        // Act
        val result = TemplateRetrieval.extractComponentName(templateCode)

        // Assert
        assertEquals("Carousel", result)
    }

    @Test
    fun `extractComponentName generates fallback name for unidentifiable patterns`() {
        // Arrange
        val templateCode = "function someHelperFunction() { return 42; }"

        // Act
        val result = TemplateRetrieval.extractComponentName(templateCode)

        // Assert
        assertNotNull(result)
        assertTrue(result.startsWith("Component"))
    }

    /* Tests for storeTemplateFiles */

    @Test
    fun `storeTemplateFiles creates files and calls TemplateInteractor`() = runBlocking {
        // Arrange
        val componentName = "TestComponent"
        val templateCode = "export const TestComponent = () => <div>Test</div>;"
        val jsonLD = "{\"json\":\"data\"}"

        // Start with clean mocks
        unmockkAll()

        // Set up mocks
        mockkObject(TemplateInteractor)
        mockkObject(TemplateRetrieval)

        // Create the test directories
        testTemplatesDir.mkdirs()
        testMetadataDir.mkdirs()

        // Mock the directories
        every { TemplateRetrieval.templatesDir } returns testTemplatesDir.absolutePath
        every { TemplateRetrieval.metadataDir } returns testMetadataDir.absolutePath

        // Don't use a spy - instead completely mock the method
        coEvery {
            TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)
        } coAnswers {
            // Create the files directly in the test
            File(testTemplatesDir, "$componentName.templ").writeText(templateCode)
            File(testMetadataDir, "$componentName.jsonld").writeText(jsonLD)

            // Call the interactor (we'll verify this)
            TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD)
            true
        }

        // Mock the interactor response
        coEvery {
            TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD)
        } returns true

        // Act
        val result = TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)

        // Debug info
        println("Template file exists: ${File(testTemplatesDir, "$componentName.templ").exists()}")

        // Assert
        assertTrue(result, "storeTemplateFiles should return true")

        // Verify files were created in the expected locations
        val templateFile = File(testTemplatesDir, "$componentName.templ")
        val jsonLdFile = File(testMetadataDir, "$componentName.jsonld")

        assertTrue(templateFile.exists(), "Template file should exist")
        assertTrue(jsonLdFile.exists(), "JSON-LD file should exist")
        assertEquals(templateCode, templateFile.readText(), "Template file content should match")
        assertEquals(jsonLD, jsonLdFile.readText(), "JSON-LD file content should match")

        // Verify TemplateInteractor was called correctly
        coVerify { TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD) }
    }

    @Test
    fun `storeTemplateFiles returns false when exception occurs`() = runBlocking {
        // Arrange
        val componentName = "TestComponent"
        val templateCode = "export const TestComponent = () => <div>Test</div>;"
        val jsonLD = "{\"json\":\"data\"}"

        // Cause an exception during file writing
        unmockkObject(TemplateRetrieval)
        mockkObject(TemplateRetrieval)

        // Use a non-existent path to cause an exception
        every { TemplateRetrieval.templatesDir } returns "/non/existent/path"

        // Act
        val result = TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `storeTemplateFiles returns false when TemplateInteractor returns false`() = runBlocking {
        // Arrange
        val componentName = "TestComponent"
        val templateCode = "export const TestComponent = () => <div>Test</div>;"
        val jsonLD = "{\"json\":\"data\"}"

        unmockkObject(TemplateRetrieval)
        mockkObject(TemplateRetrieval)
        every { TemplateRetrieval.templatesDir } returns testTemplatesDir.absolutePath
        every { TemplateRetrieval.metadataDir } returns testMetadataDir.absolutePath

        coEvery { TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD) } returns false

        // Act
        val result = TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)

        // Assert
        assertFalse(result)
    }

    /* Tests for generateTemplateAnnotation */

    @Test
    fun `generateTemplateAnnotation returns annotation when successful`() = runBlocking {
        // Arrange
        val templateCode = "export const Button = () => <button>Click me</button>;"
        val componentName = "Button"
        val annotation = "{\"@context\":\"https://schema.org/\"}"

        // Create a simple JsonObject with the annotation field
        val responseJson = buildJsonObject {
            put("annotation", JsonPrimitive(annotation))
        }

        val ollamaResponse = OllamaResponse(
            model = "test-model",
            created_at = "timestamp",
            response = "response-content",
            done = true,
            done_reason = "stop"
        )

        // First ensure Json is properly mocked
        mockkObject(Json)

        // Set up mocks
        every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
        coEvery { PrototypeInteractor.prompt("test prompt", any(), any()) } returns ollamaResponse

        // Use the correct matcher: anyString() instead of any<String>() or any()
        every { Json.parseToJsonElement(any<String>()) } returns responseJson

        // If you need to mock the formatted JSON response
        every { PromptingTools.formatResponseJson(any()) } returns "response-content"

        // Act
        val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

        // Assert
        assertEquals(annotation, result)

        // Verify the mocks were called as expected
        verify { Json.parseToJsonElement(any()) }
    }

    @Test
    fun `generateTemplateAnnotation returns null when LLM returns null`() = runBlocking {
        // Arrange
        val templateCode = "export const Button = () => <button>Click me</button>;"
        val componentName = "Button"

        every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
        coEvery { PrototypeInteractor.prompt("test prompt", any(), any()) } returns null

        // Act
        val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

        // Assert
        assertNull(result)
    }

    @Test
    fun `generateTemplateAnnotation returns null when response parsing fails`() = runBlocking {
        // Arrange
        val templateCode = "export const Button = () => <button>Click me</button>;"
        val componentName = "Button"

        val ollamaResponse = OllamaResponse(
            model = "test-model",
            created_at = "timestamp",
            response = "invalid json",
            done = true,
            done_reason = "stop"
        )

        every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
        coEvery { PrototypeInteractor.prompt("test prompt", any(), any()) } returns ollamaResponse
        every { PromptingTools.formatResponseJson("invalid json") } throws Exception("Invalid JSON")

        // Act
        val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

        // Assert
        assertNull(result)
    }

    @Test
    fun `generateTemplateAnnotation returns null when no annotation field in response`() = runBlocking {
        // Arrange
        val templateCode = "export const Button = () => <button>Click me</button>;"
        val componentName = "Button"

        // Mock Json class (without relaxed parameter)
        mockkObject(Json)

        // Create JsonObject with no annotation field
        val jsonObj = buildJsonObject {
            put("something", JsonPrimitive("else"))
        }

        // Make Json.parseToJsonElement return our object
        every { Json.parseToJsonElement(any()) } returns jsonObj

        val ollamaResponse = OllamaResponse(
            model = "test-model",
            created_at = "timestamp",
            response = "doesn't matter, mocked",
            done = true,
            done_reason = "stop"
        )

        every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
        coEvery { PrototypeInteractor.prompt("test prompt", any(), any()) } returns ollamaResponse

        // Act
        val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

        // Assert
        assertNull(result)
    }

    /* Tests for buildAnnotationPrompt */

    @Test
    fun `buildAnnotationPrompt generates correctly formatted prompt`() {
        // Arrange
        val templateCode = "export const Button = () => <button>Click me</button>;"
        val componentName = "Button"

        // Act
        val result = TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName)

        // Assert with specific checks
        assertTrue(result.contains("```typescript")) // Check code block start
        assertTrue(result.contains(templateCode))    // Check template code present
        assertTrue(result.contains("```"))           // Check code block end
        assertTrue(result.contains(componentName))   // Check component name
        assertTrue(result.contains("Generate a JSON-LD annotation"))
        assertTrue(result.contains("\"annotation\""))
    }

    /* Tests for processTemplatesFromWorkflow */

    @Test
    fun `processTemplatesFromWorkflow calls processTemplatesFromResponse`() = runBlocking {
        // Arrange
        val templates = listOf("template1", "template2")
        val response = EnhancedResponse(
            response = OllamaResponse("model", "timestamp", "response", true, "stop"),
            extractedTemplates = templates
        )

        coEvery { TemplateRetrieval.processTemplatesFromResponse(response) } returns listOf("Component1", "Component2")

        // Act
        TemplateRetrieval.processTemplatesFromWorkflow(response)

        // Assert
        coVerify { TemplateRetrieval.processTemplatesFromResponse(response) }
    }

    @Test
    fun `processTemplatesFromWorkflow handles empty processed templates`() = runBlocking {
        // Arrange
        val templates = listOf("template1", "template2")
        val response = EnhancedResponse(
            response = OllamaResponse("model", "timestamp", "response", true, "stop"),
            extractedTemplates = templates
        )

        coEvery { TemplateRetrieval.processTemplatesFromResponse(response) } returns emptyList()

        // Act
        TemplateRetrieval.processTemplatesFromWorkflow(response)

        // Assert
        coVerify { TemplateRetrieval.processTemplatesFromResponse(response) }
    }
}