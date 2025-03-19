package prompting

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
import prototype.helpers.OllamaResponse
import prototype.helpers.PromptException
import prototype.security.secureCodeCheck
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
        promptingMain = PromptingMain()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test run with successful response`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1", "keyword2"))
        val freqsPrompt = "functional requirements prompt"
        val prototypePrompt = "prototype prompt"

        val freqsResponse =
            buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
                put("keywords", JsonArray(listOf(JsonPrimitive("key1"), JsonPrimitive("key2"))))
            }

        val finalResponse =
            buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("final req1"), JsonPrimitive("final req2"))))
            }

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt

        every {
            PromptingTools.functionalRequirementsPrompt(sanitizedPrompt.prompt, sanitizedPrompt.keywords)
        } returns freqsPrompt

        every {
            PromptingTools.prototypePrompt(
                userPrompt = userPrompt,
                requirements = "req1 req2",
                templates = listOf("key1", "key2"),
            )
        } returns prototypePrompt

        coEvery {
            PrototypeInteractor.prompt(eq(freqsPrompt), any())
        } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "mock response 1",
                done = true,
                done_reason = "test",
            )

        coEvery {
            PrototypeInteractor.prompt(any(), any())
        } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "mock response",
                done = true,
                done_reason = "test",
            )

        coEvery {
            TemplateInteractor.fetchTemplates(any())
        } returns emptyList()

        every { PromptingTools.formatResponseJson("mock response") } returnsMany listOf(freqsResponse, finalResponse)

        val result = runBlocking { promptingMain.run(userPrompt) }

        assertEquals(
            "Here is your code.",
            result.chat.message,
        )
    }

    @Test
    fun `test run when LLM returns null`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"
        coEvery { PrototypeInteractor.prompt(any(), any()) } returns null
        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        assertThrows<PromptException> {
            runBlocking { promptingMain.run(userPrompt) }
        }
    }

    @Test
    fun `test run when requirements extraction fails`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        val invalidResponse =
            buildJsonObject {
                put("wrong_key", JsonPrimitive("value"))
            }

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"
        coEvery { PrototypeInteractor.prompt(any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "response",
                done = true,
                done_reason = "test",
            )
        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()
        every { PromptingTools.formatResponseJson(any()) } returns invalidResponse

        assertThrows<PromptException> {
            runBlocking { promptingMain.run(userPrompt) }
        }
    }

    @Test
    fun `test prototypePrompt with valid response`() {
        val userPrompt = "test prompt"
        val freqsResponse =
            buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
                put("keywords", JsonArray(listOf(JsonPrimitive("key1"), JsonPrimitive("key2"))))
            }

        val result =
            promptingMain::class.java
                .getDeclaredMethod("prototypePrompt", String::class.java, JsonObject::class.java, List::class.java)
                .apply { isAccessible = true }
                .invoke(promptingMain, userPrompt, freqsResponse, emptyList<String>()) as String

        // Debug logging
        println("[DEBUG_LOG] Result: $result")
        println("[DEBUG_LOG] Requirements in freqsResponse: ${(freqsResponse["requirements"] as JsonArray).joinToString(" ")}")

        // Verify that the result contains the essential parts
        assertTrue(result.contains(userPrompt), "Result should contain the user prompt")
        assertTrue(result.contains("\"req1\" \"req2\""), "Result should contain the requirements")
        // The templates are formatted as a list of numbered items in the actual implementation
        assertTrue(result.contains("1. User requirements"), "Result should contain the templates")
    }

    @Test
    fun `test prototypePrompt when requirements extraction fails`() {
        val userPrompt = "test prompt"
        val invalidResponse =
            buildJsonObject {
                put("wrong_key", JsonPrimitive("value"))
            }

        val method =
            promptingMain::class.java.getDeclaredMethod(
                "prototypePrompt",
                String::class.java,
                JsonObject::class.java,
                List::class.java,
            )
        method.isAccessible = true

        val exception =
            assertThrows<java.lang.reflect.InvocationTargetException> {
                method.invoke(promptingMain, userPrompt, invalidResponse, emptyList<String>())
            }

        assertTrue(exception.cause is PromptException)
        assertEquals("Failed to extract requirements from LLM response", exception.cause?.message)
    }

    @Test
    fun `test prototypePrompt when keywords extraction fails`() {
        val userPrompt = "test prompt"
        val invalidResponse =
            buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
                // missing keywords field
            }

        val method =
            promptingMain::class.java.getDeclaredMethod(
                "prototypePrompt",
                String::class.java,
                JsonObject::class.java,
                List::class.java,
            )
        method.isAccessible = true

        val exception =
            assertThrows<java.lang.reflect.InvocationTargetException> {
                method.invoke(promptingMain, userPrompt, invalidResponse, emptyList<String>())
            }

        assertTrue(exception.cause is PromptException)
        assertEquals("Failed to extract keywords from LLM response", exception.cause?.message)
    }

    @Test
    fun `test promptLlm with successful response`() {
        val prompt = "test prompt"
        val expectedJson =
            buildJsonObject {
                put("test", JsonPrimitive("value"))
            }

        coEvery {
            PrototypeInteractor.prompt(prompt, any())
        } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "test response",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("test response") } returns expectedJson

        val result =
            runBlocking {
                promptingMain::class.java
                    .getDeclaredMethod("promptLlm", String::class.java)
                    .apply { isAccessible = true }
                    .invoke(promptingMain, prompt) as JsonObject
            }

        assertEquals(expectedJson, result)
    }

    @Test
    fun `test promptLlm when LLM returns null`() {
        val prompt = "test prompt"

        coEvery { PrototypeInteractor.prompt(prompt, any()) } returns null

        val method = promptingMain::class.java.getDeclaredMethod("promptLlm", String::class.java)
        method.isAccessible = true

        val exception =
            assertThrows<java.lang.reflect.InvocationTargetException> {
                runBlocking {
                    method.invoke(promptingMain, prompt)
                }
            }

        assertTrue(exception.cause is PromptException)
        assertEquals("LLM did not respond!", exception.cause?.message)
    }

    @Test
    fun `test promptLlm when JSON formatting fails`() {
        val prompt = "test prompt"

        coEvery {
            PrototypeInteractor.prompt(prompt, any())
        } returns
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
                promptingMain::class.java
                    .getDeclaredMethod("promptLlm", String::class.java)
                    .apply { isAccessible = true }
                    .invoke(promptingMain, prompt)
            }
        }
    }

    @Test
    fun `test serverResponse with JsonArray`() {
        val response =
            buildJsonObject {
                putJsonArray("requirements") { 
                    add(JsonPrimitive("req1"))
                    add(JsonPrimitive("req2"))
                }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals(
            "Here is your code.",
            result.chat.message,
        )
    }

    @Test
    fun `test serverResponse with JsonPrimitive`() {
        val response =
            buildJsonObject {
                put("requirements", JsonPrimitive("single requirement"))
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals(
            "Here is your code.",
            result.chat.message,
        )
    }

    @Test
    fun `test serverResponse with empty JsonArray`() {
        val response =
            buildJsonObject {
                putJsonArray("requirements") { }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals(
            "Here is your code.",
            result.chat.message,
        )
    }

    @Test
    fun `test serverResponse with invalid type`() {
        val response =
            buildJsonObject {
                put("requirements", JsonObject(emptyMap()))
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals(
            "Here is your code.",
            result.chat.message,
        )
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
        every {
            secureCodeCheck(any(), any())
        } returns true

        val method = PromptingMain::class.java.getDeclaredMethod("onSiteSecurityCheck", LlmResponse::class.java)
        method.isAccessible = true
        method.invoke(promptingMain, llmResponse)

        verify(exactly = 2) {
            secureCodeCheck(any(), any())
        }
        unmockkStatic(::secureCodeCheck)
    }

    @Test
    fun `test onSiteSecurityCheck with unsafe code`() {
        val llmResponse =
            LlmResponse(
                mainFile = "index.js",
                files =
                    mapOf<String, FileContent>(
                        "index.js" to FileContent("console.log('Hello')"),
                        "malicious.js" to FileContent("eval('malicious code')"),
                    ),
            )

        mockkStatic(::secureCodeCheck)
        every {
            secureCodeCheck(eq("console.log('Hello')"), eq("index.js"))
        } returns true
        every {
            secureCodeCheck(eq("eval('malicious code')"), eq("malicious.js"))
        } returns false

        val method = PromptingMain::class.java.getDeclaredMethod("onSiteSecurityCheck", LlmResponse::class.java)
        method.isAccessible = true

        val exception =
            assertThrows<java.lang.reflect.InvocationTargetException> {
                method.invoke(promptingMain, llmResponse)
            }

        assertTrue(exception.cause is RuntimeException)
        assertEquals("Code is not safe for language=malicious.js", exception.cause?.message)

        verify {
            secureCodeCheck(eq("console.log('Hello')"), eq("index.js"))
            secureCodeCheck(eq("eval('malicious code')"), eq("malicious.js"))
        }
        unmockkStatic(::secureCodeCheck)
    }

    @Test
    fun `test run method flow`() {
        val userPrompt = "Create a hello world app"
        val sanitisedPrompt = SanitisedPromptResult("Create a hello world app", listOf("hello", "world"))
        val freqsPrompt = "Functional requirements prompt"

        val freqsResponse =
            buildJsonObject {
                putJsonArray("requirements") { add("Display hello world") }
                putJsonArray("keywords") {
                    add("hello")
                    add("world")
                }
            }

        val prototypeResponse =
            buildJsonObject {
                putJsonArray("requirements") { add("Display hello world") }
            }

        mockkObject(SanitisationTools)
        mockkObject(PromptingTools)
        mockkObject(PrototypeInteractor)
        mockkObject(TemplateInteractor)

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitisedPrompt
        every {
            PromptingTools.functionalRequirementsPrompt(
                sanitisedPrompt.prompt,
                sanitisedPrompt.keywords,
            )
        } returns freqsPrompt
        every { runBlocking { PrototypeInteractor.prompt(any(), any()) } } returns
            OllamaResponse(
                model = "deepseek-r1:32b",
                created_at = "2024-03-07T21:53:03Z",
                response = "response",
                done = true,
                done_reason = "stop",
            )
        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()
        every { PromptingTools.formatResponseJson("response") } returnsMany listOf(freqsResponse, prototypeResponse)

        val result = runBlocking { promptingMain.run(userPrompt) }

        verifyOrder {
            SanitisationTools.sanitisePrompt(userPrompt)
            PromptingTools.functionalRequirementsPrompt(sanitisedPrompt.prompt, sanitisedPrompt.keywords)
            runBlocking { PrototypeInteractor.prompt(freqsPrompt, "qwen2.5-coder:14b") }
            PromptingTools.formatResponseJson("response")
            PromptingTools.prototypePrompt(userPrompt, any(), any())
            runBlocking { TemplateInteractor.fetchTemplates(any()) }
            runBlocking { PrototypeInteractor.prompt(any(), "qwen2.5-coder:14b") }
            PromptingTools.formatResponseJson("response")
        }

        verify(atLeast = 1) {
            PromptingTools.prototypePrompt(userPrompt, any(), any())
        }

        assertEquals(
            "Here is your code.",
            result.chat.message,
        )

        unmockkObject(SanitisationTools)
        unmockkObject(PromptingTools)
        unmockkObject(PrototypeInteractor)
    }
}
