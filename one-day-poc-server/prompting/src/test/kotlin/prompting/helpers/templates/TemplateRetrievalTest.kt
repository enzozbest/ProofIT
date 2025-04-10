package prompting.helpers.templates

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.*
import prompting.helpers.PrototypeInteractor
import prompting.helpers.promptEngineering.PromptingTools
import prototype.helpers.OllamaResponse
import prototype.services.EnhancedResponse
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplateRetrievalTest {
    private val testDir = createTempDir("template-test")
    private val testTemplatesDir = File(testDir, "templates").apply { mkdirs() }
    private val testMetadataDir = File(testDir, "metadata").apply { mkdirs() }

    @BeforeEach
    fun setup() {
        mockkObject(PromptingTools)
        mockkObject(PrototypeInteractor)
        mockkObject(TemplateInteractor)
        mockkObject(TemplateRetrieval)

        every { TemplateRetrieval.templatesDir } returns testTemplatesDir.absolutePath
        every { TemplateRetrieval.metadataDir } returns testMetadataDir.absolutePath

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

    @Test
    fun `storeTemplateFiles returns false when storage operation throws exception`() =
        runBlocking {
            val componentName = "TestComponent"
            val templateCode = "export const TestComponent = () => <div>Test</div>;"
            val jsonLD = "{\"json\":\"data\"}"

            // Start with clean mocks
            unmockkAll()

            // Mock only what we need
            mockkObject(TemplateInteractor)

            // Use a spy on TemplateRetrieval to override only specific methods
            val retrieval = spyk(TemplateRetrieval)

            // Setup directories to exist
            every { retrieval.templatesDir } returns testTemplatesDir.absolutePath
            every { retrieval.metadataDir } returns testMetadataDir.absolutePath

            // Make sure directories exist
            testTemplatesDir.mkdirs()
            testMetadataDir.mkdirs()

            // Force an exception during the TemplateInteractor call
            coEvery {
                TemplateInteractor.storeNewTemplate(any(), any(), any())
            } throws IOException("Simulated database error")

            // This should trigger our exception in the storeTemplateFiles method
            val result = retrieval.storeTemplateFiles(componentName, templateCode, jsonLD)

            // The method should catch the exception and return false
            assertFalse(result, "storeTemplateFiles should return false when an exception occurs")

            // Verify the exception was thrown
            coVerify { TemplateInteractor.storeNewTemplate(any(), any(), any()) }
        }

    @Test
    fun `cleanTemplate delegates to PromptingTools`() {
        val rawTemplate = "```typescript\nconst Button = () => <button>Click me</button>;\n```"
        val cleanedExpected = "const Button = () => <button>Click me</button>;"

        every { PromptingTools.cleanLlmResponse(any()) } returns cleanedExpected

        val result = TemplateRetrieval.cleanTemplate(rawTemplate)

        assertEquals(cleanedExpected, result)
        verify { PromptingTools.cleanLlmResponse(rawTemplate) }
    }

    @Test
    fun `processTemplatesFromResponse returns empty list for empty templates`() =
        runBlocking {
            val response =
                EnhancedResponse(
                    response = OllamaResponse("model", "timestamp", "response", true, "stop"),
                    extractedTemplates = emptyList(),
                )

            val result = TemplateRetrieval.processTemplatesFromResponse(response)

            assertTrue(result.isEmpty())
            verify(exactly = 0) { TemplateRetrieval.createDirectoriesIfNeeded() }
        }

    @Test
    fun `processTemplatesFromResponse creates directories and processes each template`() =
        runBlocking {
            val templates =
                listOf(
                    "```tsx\nexport const Button = () => <button>Click</button>;\n```",
                    "```tsx\nexport const Card = () => <div>Card</div>;\n```",
                )

            val response =
                EnhancedResponse(
                    response = OllamaResponse("model", "timestamp", "response", true, "stop"),
                    extractedTemplates = templates,
                )

            every { TemplateRetrieval.cleanTemplate(templates[0]) } returns "export const Button = () => <button>Click</button>;"
            every { TemplateRetrieval.cleanTemplate(templates[1]) } returns "export const Card = () => <div>Card</div>;"

            coEvery {
                TemplateRetrieval.processTemplate("export const Button = () => <button>Click</button>;")
            } returns "Button"

            coEvery {
                TemplateRetrieval.processTemplate("export const Card = () => <div>Card</div>;")
            } returns "Card"

            val result = TemplateRetrieval.processTemplatesFromResponse(response)

            assertEquals(2, result.size)
            assertEquals(listOf("Button", "Card"), result)
            verify(exactly = 1) { TemplateRetrieval.createDirectoriesIfNeeded() }
            verify { TemplateRetrieval.cleanTemplate(templates[0]) }
            verify { TemplateRetrieval.cleanTemplate(templates[1]) }
        }

    @Test
    fun `processTemplatesFromResponse handles failed template processing`() =
        runBlocking {
            val templates =
                listOf(
                    "```tsx\nexport const Button = () => <button>Click</button>;\n```",
                    "```tsx\nexport const Card = () => <div>Card</div>;\n```",
                )

            val response =
                EnhancedResponse(
                    response = OllamaResponse("model", "timestamp", "response", true, "stop"),
                    extractedTemplates = templates,
                )

            every { TemplateRetrieval.cleanTemplate(any()) } answers {
                val template = firstArg<String>()
                if (template.contains("Button")) {
                    "export const Button = () => <button>Click</button>;"
                } else {
                    "export const Card = () => <div>Card</div>;"
                }
            }

            coEvery {
                TemplateRetrieval.processTemplate("export const Button = () => <button>Click</button>;")
            } returns "Button"

            coEvery {
                TemplateRetrieval.processTemplate("export const Card = () => <div>Card</div>;")
            } returns null

            val result = TemplateRetrieval.processTemplatesFromResponse(response)

            assertEquals(1, result.size)
            assertEquals("Button", result[0])
            coVerify { TemplateRetrieval.processTemplate("export const Button = () => <button>Click</button>;") }
            coVerify { TemplateRetrieval.processTemplate("export const Card = () => <div>Card</div>;") }
        }

    @Test
    fun `createDirectoriesIfNeeded creates directories`() {
        unmockkObject(TemplateRetrieval)
        mockkStatic(Files::class)
        mockkStatic(Paths::class)

        val templatePath = mockk<java.nio.file.Path>()
        val metadataPath = mockk<java.nio.file.Path>()

        every { Paths.get(TemplateRetrieval.templatesDir) } returns templatePath
        every { Paths.get(TemplateRetrieval.metadataDir) } returns metadataPath
        every { Files.createDirectories(any()) } returns mockk()

        TemplateRetrieval.createDirectoriesIfNeeded()

        verify { Files.createDirectories(templatePath) }
        verify { Files.createDirectories(metadataPath) }

        mockkObject(TemplateRetrieval)
    }

    @Test
    fun `processTemplate returns null when component name extraction fails`() =
        runBlocking {
            val templateCode = "function helper() { return 42; }"

            every { TemplateRetrieval.extractComponentName(templateCode) } returns null

            val result = TemplateRetrieval.processTemplate(templateCode)

            assertNull(result)
            verify { TemplateRetrieval.extractComponentName(templateCode) }
            coVerify(exactly = 0) { TemplateRetrieval.generateTemplateAnnotation(any(), any()) }
        }

    @Test
    fun `processTemplate returns null when JSON-LD generation fails`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click</button>;"
            val componentName = "Button"

            every { TemplateRetrieval.extractComponentName(templateCode) } returns componentName
            coEvery { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) } returns null

            val result = TemplateRetrieval.processTemplate(templateCode)

            assertNull(result)
            verify { TemplateRetrieval.extractComponentName(templateCode) }
            coVerify { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) }
            coVerify(exactly = 0) { TemplateRetrieval.storeTemplateFiles(any(), any(), any()) }
        }

    @Test
    fun `processTemplate returns null when file storage fails`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click</button>;"
            val componentName = "Button"
            val jsonLD = "{\"json\":\"data\"}"

            every { TemplateRetrieval.extractComponentName(templateCode) } returns componentName
            coEvery { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) } returns jsonLD
            coEvery { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) } returns false

            val result = TemplateRetrieval.processTemplate(templateCode)

            assertNull(result)
            verify { TemplateRetrieval.extractComponentName(templateCode) }
            coVerify { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) }
            coVerify { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) }
        }

    @Test
    fun `processTemplate returns component name when successful`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click</button>;"
            val componentName = "Button"
            val jsonLD = "{\"json\":\"data\"}"

            every { TemplateRetrieval.extractComponentName(templateCode) } returns componentName
            coEvery { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) } returns jsonLD
            coEvery { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) } returns true

            val result = TemplateRetrieval.processTemplate(templateCode)

            assertEquals(componentName, result)
            verify { TemplateRetrieval.extractComponentName(templateCode) }
            coVerify { TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName) }
            coVerify { TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD) }
        }

    @Test
    fun `extractComponentName identifies export const pattern`() {
        val templateCode = "export const Button = () => <button>Click me</button>;"

        val result = TemplateRetrieval.extractComponentName(templateCode)

        assertEquals("Button", result)
    }

    @Test
    fun `extractComponentName identifies export default function pattern`() {
        val templateCode = "export default function Card() { return <div>Card</div>; }"

        val result = TemplateRetrieval.extractComponentName(templateCode)

        assertEquals("Card", result)
    }

    @Test
    fun `extractComponentName identifies const with props pattern`() {
        val templateCode = "const Accordion = ({ items }) => { return <div>Accordion</div>; }"

        val result = TemplateRetrieval.extractComponentName(templateCode)

        assertEquals("Accordion", result)
    }

    @Test
    fun `extractComponentName identifies class component pattern`() {
        val templateCode = "class Carousel extends React.Component { render() { return <div>Carousel</div>; } }"

        val result = TemplateRetrieval.extractComponentName(templateCode)

        assertEquals("Carousel", result)
    }

    @Test
    fun `extractComponentName generates fallback name for unidentifiable patterns`() {
        val templateCode = "function someHelperFunction() { return 42; }"

        val result = TemplateRetrieval.extractComponentName(templateCode)

        assertNotNull(result)
        assertTrue(result.startsWith("Component"))
    }

    @Test
    fun `storeTemplateFiles creates files and calls TemplateInteractor`(): Unit =
        runBlocking {
            val componentName = "TestComponent"
            val templateCode = "export const TestComponent = () => <div>Test</div>;"
            val jsonLD = "{\"json\":\"data\"}"

            unmockkAll()

            mockkObject(TemplateInteractor)
            mockkObject(TemplateRetrieval)

            testTemplatesDir.mkdirs()
            testMetadataDir.mkdirs()

            every { TemplateRetrieval.templatesDir } returns testTemplatesDir.absolutePath
            every { TemplateRetrieval.metadataDir } returns testMetadataDir.absolutePath

            coEvery {
                TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)
            } coAnswers {
                File(testTemplatesDir, "$componentName.templ").writeText(templateCode)
                File(testMetadataDir, "$componentName.jsonld").writeText(jsonLD)

                TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD)
                true
            }

            coEvery {
                TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD)
            } returns true

            val result = TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)

            println("Template file exists: ${File(testTemplatesDir, "$componentName.templ").exists()}")

            assertTrue(result, "storeTemplateFiles should return true")

            val templateFile = File(testTemplatesDir, "$componentName.templ")
            val jsonLdFile = File(testMetadataDir, "$componentName.jsonld")

            assertTrue(templateFile.exists(), "Template file should exist")
            assertTrue(jsonLdFile.exists(), "JSON-LD file should exist")
            assertEquals(templateCode, templateFile.readText(), "Template file content should match")
            assertEquals(jsonLD, jsonLdFile.readText(), "JSON-LD file content should match")

            coVerify { TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD) }
        }

    @Test
    fun `storeTemplateFiles returns false when exception occurs`() =
        runBlocking {
            val componentName = "TestComponent"
            val templateCode = "export const TestComponent = () => <div>Test</div>;"
            val jsonLD = "{\"json\":\"data\"}"

            unmockkObject(TemplateRetrieval)
            mockkObject(TemplateRetrieval)

            every { TemplateRetrieval.templatesDir } returns "/non/existent/path"

            val result = TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)

            assertFalse(result)
        }

    @Test
    fun `storeTemplateFiles returns false when TemplateInteractor returns false`() =
        runBlocking {
            val componentName = "TestComponent"
            val templateCode = "export const TestComponent = () => <div>Test</div>;"
            val jsonLD = "{\"json\":\"data\"}"

            unmockkObject(TemplateRetrieval)
            mockkObject(TemplateRetrieval)
            every { TemplateRetrieval.templatesDir } returns testTemplatesDir.absolutePath
            every { TemplateRetrieval.metadataDir } returns testMetadataDir.absolutePath

            coEvery { TemplateInteractor.storeNewTemplate(componentName, templateCode, jsonLD) } returns false

            val result = TemplateRetrieval.storeTemplateFiles(componentName, templateCode, jsonLD)

            assertFalse(result)
        }

    @Test
    fun `generateTemplateAnnotation returns annotation when successful`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"
            val annotation = "{\"@context\":\"https://schema.org/\"}"

            val responseJson =
                buildJsonObject {
                    put("annotation", JsonPrimitive(annotation))
                }

            val ollamaResponse =
                OllamaResponse(
                    model = "test-model",
                    created_at = "timestamp",
                    response = "response-content",
                    done = true,
                    done_reason = "stop",
                )

            mockkObject(Json)

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns ollamaResponse

            every { Json.parseToJsonElement(any<String>()) } returns responseJson

            every { PromptingTools.formatResponseJson(any()) } returns "response-content"

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertEquals(annotation, result)

            verify { Json.parseToJsonElement(any()) }
        }

    @Test
    fun `generateTemplateAnnotation returns null when LLM returns null`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns null

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertNull(result)
        }

    @Test
    fun `generateTemplateAnnotation returns null when response parsing fails`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"

            val ollamaResponse =
                OllamaResponse(
                    model = "test-model",
                    created_at = "timestamp",
                    response = "invalid json",
                    done = true,
                    done_reason = "stop",
                )

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns ollamaResponse
            every { PromptingTools.formatResponseJson("invalid json") } throws Exception("Invalid JSON")

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertNull(result)
        }

    @Test
    fun `generateTemplateAnnotation returns null when no annotation field in response`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"

            mockkObject(Json)

            val jsonObj =
                buildJsonObject {
                    put("something", JsonPrimitive("else"))
                }

            every { Json.parseToJsonElement(any()) } returns jsonObj

            val ollamaResponse =
                OllamaResponse(
                    model = "test-model",
                    created_at = "timestamp",
                    response = "doesn't matter, mocked",
                    done = true,
                    done_reason = "stop",
                )

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns ollamaResponse

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertNull(result)
        }

    // Tests for buildAnnotationPrompt

    @Test
    fun `buildAnnotationPrompt generates correctly formatted prompt`() {
        val templateCode = "export const Button = () => <button>Click me</button>;"
        val componentName = "Button"

        val result = TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName)

        assertTrue(result.contains("```typescript")) // Check code block start
        assertTrue(result.contains(templateCode)) // Check template code present
        assertTrue(result.contains("```")) // Check code block end
        assertTrue(result.contains(componentName)) // Check component name
        assertTrue(result.contains("Generate a JSON-LD annotation"))
        assertTrue(result.contains("\"annotation\""))
    }

    @Test
    fun `processTemplatesFromWorkflow calls processTemplatesFromResponse`() =
        runBlocking {
            val templates = listOf("template1", "template2")
            val response =
                EnhancedResponse(
                    response = OllamaResponse("model", "timestamp", "response", true, "stop"),
                    extractedTemplates = templates,
                )

            coEvery { TemplateRetrieval.processTemplatesFromResponse(response) } returns
                listOf(
                    "Component1",
                    "Component2",
                )

            TemplateRetrieval.processTemplatesFromWorkflow(response)

            coVerify { TemplateRetrieval.processTemplatesFromResponse(response) }
        }

    @Test
    fun `processTemplatesFromWorkflow handles empty processed templates`() =
        runBlocking {
            val templates = listOf("template1", "template2")
            val response =
                EnhancedResponse(
                    response = OllamaResponse("model", "timestamp", "response", true, "stop"),
                    extractedTemplates = templates,
                )

            coEvery { TemplateRetrieval.processTemplatesFromResponse(response) } returns emptyList()

            TemplateRetrieval.processTemplatesFromWorkflow(response)

            coVerify { TemplateRetrieval.processTemplatesFromResponse(response) }
        }

    @Test
    fun `generateTemplateAnnotation handles JsonArray response with annotation`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"
            val annotation = "{\"@context\":\"https://schema.org/\"}"

            // Create a JsonArray response with an annotation
            val jsonArray =
                buildJsonArray {
                    addJsonObject {
                        put("annotation", JsonPrimitive(annotation))
                    }
                }

            val ollamaResponse =
                OllamaResponse(
                    model = "test-model",
                    created_at = "timestamp",
                    response = "[{\"annotation\":\"$annotation\"}]",
                    done = true,
                    done_reason = "stop",
                )

            mockkObject(Json)

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns ollamaResponse
            every { Json.parseToJsonElement(any<String>()) } returns jsonArray
            every { PromptingTools.formatResponseJson(any()) } returns ollamaResponse.response.toString()

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertEquals(annotation, result)
            verify { Json.parseToJsonElement(any()) }
        }

    @Test
    fun `generateTemplateAnnotation returns null when JsonArray is empty`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"

            // Create an empty JsonArray
            val emptyJsonArray = buildJsonArray {}

            val ollamaResponse =
                OllamaResponse(
                    model = "test-model",
                    created_at = "timestamp",
                    response = "[]", // Empty array
                    done = true,
                    done_reason = "stop",
                )

            mockkObject(Json)

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns ollamaResponse
            every { Json.parseToJsonElement(any<String>()) } returns emptyJsonArray
            every { PromptingTools.formatResponseJson(any()) } returns ollamaResponse.response.toString()

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertNull(result)
            verify { Json.parseToJsonElement(any()) }
        }

    @Test
    fun `generateTemplateAnnotation returns null when JsonArray item has no annotation field`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"

            // Create a JsonArray without an annotation field
            val jsonArray =
                buildJsonArray {
                    addJsonObject {
                        put("something", JsonPrimitive("else"))
                    }
                }

            val ollamaResponse =
                OllamaResponse(
                    model = "test-model",
                    created_at = "timestamp",
                    response = "[{\"something\":\"else\"}]",
                    done = true,
                    done_reason = "stop",
                )

            mockkObject(Json)

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns ollamaResponse
            every { Json.parseToJsonElement(any<String>()) } returns jsonArray
            every { PromptingTools.formatResponseJson(any()) } returns ollamaResponse.response.toString()

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertNull(result)
            verify { Json.parseToJsonElement(any()) }
        }

    @Test
    fun `generateTemplateAnnotation returns null for unexpected JSON format`() =
        runBlocking {
            val templateCode = "export const Button = () => <button>Click me</button>;"
            val componentName = "Button"

            // Create a JsonPrimitive (neither object nor array)
            val jsonPrimitive = JsonPrimitive("unexpected format")

            val ollamaResponse =
                OllamaResponse(
                    model = "test-model",
                    created_at = "timestamp",
                    response = "\"unexpected format\"",
                    done = true,
                    done_reason = "stop",
                )

            mockkObject(Json)

            every { TemplateRetrieval.buildAnnotationPrompt(templateCode, componentName) } returns "test prompt"
            coEvery { PrototypeInteractor.prompt("test prompt", any(), any(), any()) } returns ollamaResponse
            every { Json.parseToJsonElement(any<String>()) } returns jsonPrimitive
            every { PromptingTools.formatResponseJson(any()) } returns ollamaResponse.response.toString()

            val result = TemplateRetrieval.generateTemplateAnnotation(templateCode, componentName)

            assertNull(result)
            verify { Json.parseToJsonElement(any()) }
        }
}
