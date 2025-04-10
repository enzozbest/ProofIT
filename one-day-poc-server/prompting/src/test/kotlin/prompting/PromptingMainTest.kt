package prompting

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import prompting.helpers.PromptFormatter
import prompting.helpers.PromptFormatterFactory
import prompting.helpers.PrototypeInteractor
import prompting.helpers.ResponseFormatter
import prompting.helpers.ResponseFormatterFactory
import prompting.helpers.promptEngineering.PromptingTools
import prompting.helpers.promptEngineering.SanitisationTools
import prompting.helpers.promptEngineering.SanitisedPromptResult
import prompting.helpers.templates.TemplateInteractor
import prototype.FileContent
import prototype.LlmResponse
import prototype.helpers.LLMOptions
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse
import prototype.helpers.PromptException
import prototype.security.secureCodeCheck
import prototype.services.OllamaService
import utils.environment.EnvironmentLoader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PromptingMainTest {
    private lateinit var promptingMain: PromptingMain

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(SanitisationTools)
        mockkObject(PromptingTools)
        mockkObject(PrototypeInteractor)
        mockkObject(TemplateInteractor)
        mockkObject(OllamaService)
        promptingMain = PromptingMain()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test run with successful response`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt =
            SanitisedPromptResult("sanitized test prompt", listOf("keyword1", "keyword2"))
        val freqsPrompt = "functional requirements prompt"
        val prototypePrompt = "prototype prompt"

        val freqsResponseString =
            """
            {
                "requirements": ["req1", "req2"],
                "keywords": ["key1", "key2"]
            }
            """.trimIndent()

        val finalResponseString =
            """
            {
                "chat": {
                    "message": "Here is your code."
                },
                "prototype": {
                    "files": {}
                }
            }
            """.trimIndent()

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt

        every {
            PromptingTools.functionalRequirementsPrompt(
                sanitizedPrompt.prompt,
                sanitizedPrompt.keywords,
            )
        } returns freqsPrompt

        every {
            PromptingTools.ollamaPrompt(
                userPrompt = userPrompt,
                requirements = "req1 req2",
                templates = listOf("key1", "key2"),
            )
        } returns prototypePrompt

        coEvery { OllamaService.generateResponse(any(), any(), any()) } returns
            Result.success(
                OllamaResponse(
                    model = "test-model",
                    created_at = "2024-01-01",
                    response = "{\"key\": \"value\"}",
                    done = true,
                    done_reason = "test",
                ),
            )

        coEvery { PrototypeInteractor.prompt(any(), any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "{\"key\": \"value\"}",
                done = true,
                done_reason = "test",
            )

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        every { PromptingTools.formatResponseJson(any()) } returnsMany
            listOf(freqsResponseString, finalResponseString)

        val result = runBlocking { promptingMain.run(userPrompt) }

        assertTrue(
            result.contains("Here is your code."),
            "Result should contain the expected message",
        )
    }

    @Test
    fun `test run when LLM returns null`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(any(), any(), any(), any()) } returns null

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val exception =
            assertThrows<PromptException> { runBlocking { promptingMain.run(userPrompt) } }

        assertEquals("LLM did not respond!", exception.message)
    }

    @Test
    fun `test run when requirements extraction fails`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        buildJsonObject { put("wrong_key", JsonPrimitive("value")) }

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(eq("prompt"), any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "response",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("response") } returns """{"wrong_key": "value"}"""

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val exception =
            assertThrows<PromptException> { runBlocking { promptingMain.run(userPrompt) } }

        assertEquals("Failed to extract requirements from LLM response", exception.message)
    }

    @Test
    fun `test run when keywords extraction fails`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(eq("prompt"), any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "response",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("response") } returns
            """{"requirements": ["req1", "req2"]}"""

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val exception =
            assertThrows<PromptException> { runBlocking { promptingMain.run(userPrompt) } }

        assertEquals("Failed to extract keywords from LLM response", exception.message)
    }

    @Test
    fun `test prototypePrompt with valid inputs`() {
        val userPrompt = "test prompt"
        val freqsResponse =
            buildJsonObject {
                putJsonArray("requirements") {
                    add(JsonPrimitive("req1"))
                    add(JsonPrimitive("req2"))
                }
                putJsonArray("keywords") {
                    add(JsonPrimitive("key1"))
                    add(JsonPrimitive("key2"))
                }
            }

        mockkStatic(PromptingTools::class)

        every {
            PromptingTools.ollamaPrompt(
                userPrompt = any(),
                requirements = any(),
                templates = emptyList(),
            )
        } returns "mocked prototype prompt"

        val result = testMethod(userPrompt, freqsResponse)

        assertEquals("mocked prototype prompt", result)

        verify(exactly = 1) {
            PromptingTools.ollamaPrompt(
                userPrompt = userPrompt,
                requirements = any(),
                templates = emptyList(),
            )
        }

        unmockkStatic(PromptingTools::class)
    }

    @Test
    fun `test promptLlm with successful response`() {
        val prompt = "test prompt"
        val expectedJsonString = """{"key": "value"}"""

        coEvery { PrototypeInteractor.prompt(prompt, any(), "local", OllamaOptions()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "test response",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("test response") } returns expectedJsonString

        val result =
            runBlocking {
                promptingMain::class
                    .java
                    .getDeclaredMethod("promptLlm", String::class.java, LLMOptions::class.java, String::class.java)
                    .apply { isAccessible = true }
                    .invoke(promptingMain, prompt, OllamaOptions(), "local") as
                    String
            }

        assertEquals(expectedJsonString, result)
    }

    @Test
    fun `test promptLlm when LLM returns null`() {
        val prompt = "test prompt"

        coEvery { PrototypeInteractor.prompt(prompt, any(), "local", OllamaOptions()) } returns null

        val method =
            promptingMain::class.java.getDeclaredMethod(
                "promptLlm",
                String::class.java,
                LLMOptions::class.java,
                String::class.java,
            )
        method.isAccessible = true

        val exception =
            assertThrows<java.lang.reflect.InvocationTargetException> {
                runBlocking { method.invoke(promptingMain, prompt, OllamaOptions(), "local") }
            }

        assertTrue(exception.cause is PromptException)
        assertEquals("LLM did not respond!", exception.cause?.message)
    }

    @Test
    fun `test promptLlm when JSON formatting fails`() {
        val prompt = "test prompt"

        coEvery { PrototypeInteractor.prompt(prompt, any(), "local", OllamaOptions()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "invalid json",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("invalid json") } throws Exception("Invalid JSON")

        assertThrows<Exception> {
            runBlocking {
                promptingMain::class
                    .java
                    .getDeclaredMethod(
                        "promptLlm",
                        String::class.java,
                        OllamaOptions::class.java,
                        String::class.java,
                    ).apply { isAccessible = true }
                    .invoke(promptingMain, prompt, OllamaOptions(), "local")
            }
        }
    }

    @Test
    fun `test promptLlm with default options parameter`() {
        val prompt = "test prompt"
        val expectedJsonString = """{"test": "value"}"""

        val optionsSlot = slot<OllamaOptions>()

        coEvery { PrototypeInteractor.prompt(eq(prompt), any(), "local", capture(optionsSlot)) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "test response",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("test response") } returns expectedJsonString

        val testPromptingMain = PromptingMain("test-model")

        val method =
            testPromptingMain::class.java.getDeclaredMethod(
                "promptLlm",
                String::class.java,
                LLMOptions::class.java,
                String::class.java,
            )
        method.isAccessible = true

        val result =
            runBlocking {
                method.invoke(testPromptingMain, prompt, OllamaOptions(), "local") as String
            }

        assertEquals(expectedJsonString, result)

        coVerify(exactly = 1) { PrototypeInteractor.prompt(eq(prompt), eq("test-model"), "local", any()) }
    }

    @Test
    fun `test onSiteSecurityCheck with safe code`() {
        val llmResponse =
            LlmResponse(
                mainFile = "index.js",
                files =
                    mapOf<String, FileContent>(
                        "index.js" to FileContent("console.log('Hello')"),
                        "style.css" to FileContent("body { color: black; }"),
                    ),
            )

        mockkStatic(::secureCodeCheck)

        every { secureCodeCheck(any(), any()) } returns true

        val method =
            promptingMain::class.java.getDeclaredMethod(
                "onSiteSecurityCheck",
                LlmResponse::class.java,
            )
        method.isAccessible = true

        // This should not throw an exception
        method.invoke(promptingMain, llmResponse)

        verify(exactly = 2) { secureCodeCheck(any(), any()) }

        unmockkStatic(::secureCodeCheck)
    }

    @Test
    fun `test onSiteSecurityCheck with unsafe code`() {
        val llmResponse =
            LlmResponse(
                mainFile = "index.js",
                files =
                    mapOf<String, FileContent>(
                        "index.js" to FileContent("eval('alert(1)')"),
                        "style.css" to FileContent("body { color: black; }"),
                    ),
            )

        mockkStatic(::secureCodeCheck)

        every { secureCodeCheck("eval('alert(1)')", "index.js") } returns false
        every { secureCodeCheck("body { color: black; }", "style.css") } returns true

        val method =
            promptingMain::class.java.getDeclaredMethod(
                "onSiteSecurityCheck",
                LlmResponse::class.java,
            )
        method.isAccessible = true

        val exception =
            assertThrows<java.lang.reflect.InvocationTargetException> {
                method.invoke(promptingMain, llmResponse)
            }

        assertTrue(exception.cause is RuntimeException)
        assertEquals("Code is not safe for language=index.js", exception.cause?.message)

        verify(exactly = 1) { secureCodeCheck("eval('alert(1)')", "index.js") }
        verify(exactly = 0) { secureCodeCheck("body { color: black; }", "style.css") }

        unmockkStatic(::secureCodeCheck)
    }

    @Test
    fun `test run with real world example`() {
        val userPrompt = "Create a hello world app"
        val modelName = "deepseek-r1:32b"
        val sanitisedPrompt = SanitisedPromptResult("Create a hello world app", listOf("hello", "world"))
        val freqsPrompt = "Functional requirements prompt"

        mockkObject(SanitisationTools)
        mockkObject(PromptingTools)
        mockkObject(PrototypeInteractor)
        mockkObject(TemplateInteractor)

        val testPromptingMain = PromptingMain(modelName)

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitisedPrompt
        every {
            PromptingTools.functionalRequirementsPrompt(
                sanitisedPrompt.prompt,
                sanitisedPrompt.keywords,
            )
        } returns freqsPrompt

        coEvery { PrototypeInteractor.prompt(any(), any(), any(), any()) } returns
            OllamaResponse(
                model = "deepseek-r1:32b",
                created_at = "2024-03-07T21:53:03Z",
                response = "{\"key\": \"value\"}",
                done = true,
                done_reason = "stop",
            )

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        every { PromptingTools.formatResponseJson(any()) } returnsMany
            listOf(
                """{"requirements": ["Display hello world"], "keywords": ["hello", "world"]}""",
                """{"requirements": ["Display hello world"]}""",
            )

        val result = runBlocking { testPromptingMain.run(userPrompt) }

        verify {
            SanitisationTools.sanitisePrompt(userPrompt)
            PromptingTools.functionalRequirementsPrompt(
                sanitisedPrompt.prompt,
                sanitisedPrompt.keywords,
            )
            PromptingTools.formatResponseJson(any())
            PromptingTools.ollamaPrompt(userPrompt, any(), any())
        }

        coVerify {
            PrototypeInteractor.prompt(freqsPrompt, modelName, "local", any())
            TemplateInteractor.fetchTemplates(any())
            PrototypeInteractor.prompt(any(), modelName, "local", any())
        }

        verify(atLeast = 1) { PromptingTools.ollamaPrompt(userPrompt, any(), any()) }

        assertTrue(result.contains("Display hello world"), "Result should contain the requirements")

        unmockkObject(SanitisationTools)
        unmockkObject(PromptingTools)
        unmockkObject(PrototypeInteractor)
        unmockkObject(TemplateInteractor)
    }

    /**
     * Helper method to test the private prototypePrompt method
     */
    private fun testMethod(
        userPrompt: String,
        freqsResponse: JsonObject,
    ): String =
        runBlocking {
            promptingMain::class.java
                .getDeclaredMethod(
                    "prototypePrompt",
                    String::class.java,
                    JsonObject::class.java,
                    List::class.java,
                    String::class.java,
                    String::class.java,
                ).apply { isAccessible = true }
                .invoke(
                    promptingMain,
                    userPrompt,
                    freqsResponse,
                    emptyList<String>(),
                    null,
                    "local",
                ) as String
        }

    @Test
    fun `test PrototypeResponse data class`() {
        val files = """{"file1": "content1", "file2": "content2"}"""
        val response = PrototypeResponse(files)

        assertEquals(files, response.files)
    }

    @Test
    fun `test ChatResponse data class`() {
        val message = "Hello, world!"
        val role = "Assistant"
        val timestamp = "2023-01-01T12:00:00Z"
        val messageId = "msg123"

        val response = ChatResponse(message, role, timestamp, messageId)

        assertEquals(message, response.message)
        assertEquals(role, response.role)
        assertEquals(timestamp, response.timestamp)
        assertEquals(messageId, response.messageId)
    }

    @Test
    fun `test ChatResponse data class with default role`() {
        val message = "Hello, world!"
        val timestamp = "2023-01-01T12:00:00Z"
        val messageId = "msg123"

        val response = ChatResponse(message = message, timestamp = timestamp, messageId = messageId)

        assertEquals(message, response.message)
        assertEquals("LLM", response.role)
        assertEquals(timestamp, response.timestamp)
        assertEquals(messageId, response.messageId)
    }

    @Test
    fun `test prototypePrompt with all parameters`() {
        val userPrompt = "test prompt"
        val freqsResponse =
            buildJsonObject {
                putJsonArray("requirements") {
                    add(JsonPrimitive("req1"))
                    add(JsonPrimitive("req2"))
                }
                putJsonArray("keywords") {
                    add(JsonPrimitive("key1"))
                    add(JsonPrimitive("key2"))
                }
            }
        val templates = listOf("template1", "template2")
        val previousGeneration = "previous generation"

        mockkObject(PromptFormatterFactory)
        val mockFormatter = mockk<PromptFormatter>()
        every { mockFormatter.format(userPrompt, any(), templates, previousGeneration) } returns "formatted prompt"
        every { PromptFormatterFactory.getFormatter("openai") } returns mockFormatter

        val result =
            runBlocking {
                promptingMain::class.java
                    .getDeclaredMethod(
                        "prototypePrompt",
                        String::class.java,
                        JsonObject::class.java,
                        List::class.java,
                        String::class.java,
                        String::class.java,
                    ).apply { isAccessible = true }
                    .invoke(
                        promptingMain,
                        userPrompt,
                        freqsResponse,
                        templates,
                        previousGeneration,
                        "openai",
                    ) as String
            }

        assertEquals("formatted prompt", result)
        verify(exactly = 1) {
            mockFormatter.format(
                userPrompt,
                any(),
                templates,
                previousGeneration,
            )
        }

        unmockkObject(PromptFormatterFactory)
    }

    @Test
    fun `test promptLlm with openai route`() {
        val prompt = "test prompt"
        val expectedJsonString = """{"key": "value"}"""

        mockkObject(ResponseFormatterFactory)
        val mockFormatter = mockk<ResponseFormatter>()
        every { mockFormatter.format(any()) } returns expectedJsonString
        every { ResponseFormatterFactory.getFormatter("openai") } returns mockFormatter

        coEvery { PrototypeInteractor.prompt(prompt, any(), "openai", any()) } returns
            OpenAIResponse(
                model = "test-model",
                createdAt = 1672531200,
                response = "test response",
            )

        val result =
            runBlocking {
                promptingMain::class.java
                    .getDeclaredMethod(
                        "promptLlm",
                        String::class.java,
                        LLMOptions::class.java,
                        String::class.java,
                    ).apply { isAccessible = true }
                    .invoke(promptingMain, prompt, OpenAIOptions(), "openai") as String
            }

        assertEquals(expectedJsonString, result)
        verify(exactly = 1) { mockFormatter.format(any()) }

        unmockkObject(ResponseFormatterFactory)
    }

    @Test
    fun `test run with invalid JSON response`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(any(), any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "invalid json",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("invalid json") } returns """{"requirements": [], "keywords": []}"""

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val result = runBlocking { promptingMain.run(userPrompt) }

        assertEquals("""{"requirements": [], "keywords": []}""", result)
    }

    @Test
    fun `test run with JSON that cannot be parsed`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(any(), any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "not a json",
                done = true,
                done_reason = "test",
            )

        // This will cause Json.decodeFromString to throw an exception
        every { PromptingTools.formatResponseJson("not a json") } returns "not a json"

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val exception =
            assertThrows<PromptException> {
                runBlocking { promptingMain.run(userPrompt) }
            }

        assertEquals("Failed to extract requirements from LLM response", exception.message)
    }

    @Test
    fun `test run with JSON missing requirements key`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(any(), any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "json without requirements",
                done = true,
                done_reason = "test",
            )

        // JSON without requirements key
        every { PromptingTools.formatResponseJson("json without requirements") } returns """{"keywords": ["key1"]}"""

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val exception =
            assertThrows<PromptException> {
                runBlocking { promptingMain.run(userPrompt) }
            }

        assertEquals("Failed to extract requirements from LLM response", exception.message)
    }

    @Test
    fun `test run with requirements as empty array`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(eq("prompt"), any(), eq("local"), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "json with empty requirements array",
                done = true,
                done_reason = "test",
            )

        // JSON with empty requirements array
        every { PromptingTools.formatResponseJson("json with empty requirements array") } returns
            """{"requirements": [], "keywords": ["key1"]}"""

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        mockkObject(PromptFormatterFactory)
        val mockFormatter = mockk<PromptFormatter>()
        every { mockFormatter.format(any(), eq(""), any(), any()) } returns "formatted prompt"
        every { PromptFormatterFactory.getFormatter(any()) } returns mockFormatter

        coEvery { PrototypeInteractor.prompt(eq("formatted prompt"), any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "final response",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("final response") } returns "final formatted response"

        val result = runBlocking { promptingMain.run(userPrompt) }

        assertEquals("final formatted response", result)
        verify(exactly = 1) { mockFormatter.format(any(), eq(""), any(), any()) }

        unmockkObject(PromptFormatterFactory)
    }

    @Test
    fun `test run with USE_OPENAI set to false`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        mockkObject(EnvironmentLoader)
        every { EnvironmentLoader.get("USE_OPENAI") } returns "false"

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(any(), any(), eq("local"), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = """{"requirements": ["req1"], "keywords": ["key1"]}""",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson(any()) } returns """{"requirements": ["req1"], "keywords": ["key1"]}"""

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        runBlocking { promptingMain.run(userPrompt) }

        coVerify { PrototypeInteractor.prompt(any(), any(), "local", any()) }

        unmockkObject(EnvironmentLoader)
    }

    @Test
    fun `test run with USE_OPENAI set to true`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        mockkObject(EnvironmentLoader)
        every { EnvironmentLoader.get("USE_OPENAI") } returns "true"

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(any(), any(), eq("local"), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = """{"requirements": ["req1"], "keywords": ["key1"]}""",
                done = true,
                done_reason = "test",
            )

        coEvery { PrototypeInteractor.prompt(any(), any(), eq("openai"), any()) } returns
            OpenAIResponse(
                model = "test-model",
                createdAt = 1672531200,
                response = """{"requirements": ["req1"], "keywords": ["key1"]}""",
            )

        every { PromptingTools.formatResponseJson(any()) } returns """{"requirements": ["req1"], "keywords": ["key1"]}"""

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        runBlocking { promptingMain.run(userPrompt) }

        coVerify { PrototypeInteractor.prompt(any(), any(), "openai", any()) }

        unmockkStatic(EnvironmentLoader::class)
    }

    @Test
    fun `test serialisation and deserialisation of PrototypeResponse`() {
        val files = """{"file1": "content1", "file2": "content2"}"""
        val response = PrototypeResponse(files)

        val jsonString = Json.encodeToString(PrototypeResponse.serializer(), response)
        val deserializedResponse = Json.decodeFromString(PrototypeResponse.serializer(), jsonString)

        assertEquals(response.files, deserializedResponse.files)
    }

    @Test
    fun `test serialisation and deserialisation of PrototypeResponse with missing field`() {
        val jsonString = """{}"""
        assertFailsWith<SerializationException> { Json.decodeFromString(PrototypeResponse.serializer(), jsonString) }
    }
}
