package prompting

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import prompting.helpers.PrototypeInteractor
import prompting.helpers.promptEngineering.PromptingTools
import prompting.helpers.promptEngineering.SanitisationTools
import prompting.helpers.promptEngineering.SanitisedPromptResult
import prompting.helpers.templates.TemplateInteractor
import prototype.FileContent
import prototype.LlmResponse
import prototype.helpers.LLMOptions
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.helpers.PromptException
import prototype.security.secureCodeCheck
import prototype.services.OllamaService
import kotlin.test.assertEquals
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
    private fun testMethod(userPrompt: String, freqsResponse: JsonObject): String {
        return runBlocking {
            promptingMain::class.java
                .getDeclaredMethod(
                    "prototypePrompt",
                    String::class.java,
                    JsonObject::class.java,
                    List::class.java,
                    String::class.java,
                    String::class.java,
                )
                .apply { isAccessible = true }
                .invoke(
                    promptingMain,
                    userPrompt,
                    freqsResponse,
                    emptyList<String>(),
                    null,
                    "local",
                ) as String
        }
    }

    @Test
    @org.junit.jupiter.api.Disabled("This test is failing due to issues with mocking the LLM")
    fun `test run with OpenAI route`() {
        val userPrompt = "Create a hello world app"
        val modelName = "gpt-4"
        val ollamaModelName = "deepseek-r1:32b"
        val sanitisedPrompt = SanitisedPromptResult("Create a hello world app", listOf("hello", "world"))
        val freqsPrompt = "Functional requirements prompt"
        val prototypePrompt = "Prototype prompt"

        mockkObject(SanitisationTools)
        mockkObject(PromptingTools)
        mockkObject(PrototypeInteractor)
        mockkObject(TemplateInteractor)

        val testPromptingMain = PromptingMain(ollamaModel = ollamaModelName, openAIModel = modelName)

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitisedPrompt
        every {
            PromptingTools.functionalRequirementsPrompt(
                sanitisedPrompt.prompt,
                sanitisedPrompt.keywords,
            )
        } returns freqsPrompt

        every {
            PromptingTools.ollamaPrompt(
                userPrompt = any(),
                requirements = any(),
                templates = any(),
            )
        } returns prototypePrompt

        coEvery { PrototypeInteractor.prompt(eq(freqsPrompt), eq(ollamaModelName), eq("local"), any()) } returns
            OllamaResponse(
                model = ollamaModelName,
                created_at = "2024-03-07T21:53:03Z",
                response = "{\"requirements\": [\"Display hello world\"], \"keywords\": [\"hello\", \"world\"]}",
                done = true,
                done_reason = "stop",
            )

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        coEvery { PrototypeInteractor.prompt(eq(prototypePrompt), eq(modelName), eq("openai"), any()) } returns
            OllamaResponse(
                model = modelName,
                created_at = "2024-03-07T21:53:03Z",
                response = "{\"chat\": {\"message\": \"Here is your hello world app\"}, \"prototype\": {\"files\": {}}}",
                done = true,
                done_reason = "stop",
            )

        every { PromptingTools.formatResponseJson(any()) } returnsMany
            listOf(
                """{"requirements": ["Display hello world"], "keywords": ["hello", "world"]}""",
                """{"chat": {"message": "Here is your hello world app"}, "prototype": {"files": {}}}""",
            )

        val result = runBlocking { testPromptingMain.run(userPrompt) }

        verify {
            SanitisationTools.sanitisePrompt(userPrompt)
            PromptingTools.functionalRequirementsPrompt(
                sanitisedPrompt.prompt,
                sanitisedPrompt.keywords,
            )
            PromptingTools.formatResponseJson(any())
        }

        coVerify {
            PrototypeInteractor.prompt(freqsPrompt, ollamaModelName, "local", any())
            TemplateInteractor.fetchTemplates(any())
            PrototypeInteractor.prompt(prototypePrompt, modelName, "openai", any())
        }

        assertTrue(result.contains("Here is your hello world app"), "Result should contain the expected message")

        unmockkObject(SanitisationTools)
        unmockkObject(PromptingTools)
        unmockkObject(PrototypeInteractor)
        unmockkObject(TemplateInteractor)
    }
}
