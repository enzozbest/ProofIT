package kcl.seg.rtt.prompting

import helpers.SanitisationTools
import helpers.SanitisedPromptResult
import io.mockk.*
import kcl.seg.rtt.prompting.helpers.PromptingTools
import kcl.seg.rtt.prompting.prototypeInteraction.PrototypeInteractor
import kcl.seg.rtt.prototype.FileContent
import kcl.seg.rtt.prototype.LlmResponse
import kcl.seg.rtt.prototype.OllamaResponse
import kcl.seg.rtt.prototype.PromptException
import kcl.seg.rtt.prototype.secureCodeCheck
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
                templates = "key1 key2",
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
        } returns OllamaResponse(
            model = "test-model",
            created_at = "2024-01-01",
            response = "mock response",
            done = true,
            done_reason = "test"
        )

        every { PromptingTools.formatResponseJson("mock response") } returnsMany listOf(freqsResponse, finalResponse)

        val result = promptingMain.run(userPrompt)

        assertEquals(
            "These are the functional requirements fulfilled by this prototype: final req1, final req2",
            result.response,
        )
    }

    @Test
    fun `test run when LLM returns null`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"
        coEvery { PrototypeInteractor.prompt(any(), any()) } returns null

        assertThrows<PromptException> {
            promptingMain.run(userPrompt)
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
        every { PromptingTools.formatResponseJson(any()) } returns invalidResponse

        assertThrows<PromptException> {
            promptingMain.run(userPrompt)
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
                .getDeclaredMethod("prototypePrompt", String::class.java, JsonObject::class.java)
                .apply { isAccessible = true }
                .invoke(promptingMain, userPrompt, freqsResponse) as String

        // Verify that the result contains the essential parts
        assertTrue(result.contains(userPrompt), "Result should contain the user prompt")
        assertTrue(result.contains("\"\"req1\" \"req2\"\""), "Result should contain the requirements")
        assertTrue(result.contains("\"\"key1\" \"key2\"\""), "Result should contain the templates")
    }

    @Test
    fun `test prototypePrompt when requirements extraction fails`() {
        val userPrompt = "test prompt"
        val invalidResponse =
            buildJsonObject {
                put("wrong_key", JsonPrimitive("value"))
            }

        val method = promptingMain::class.java.getDeclaredMethod("prototypePrompt", String::class.java, JsonObject::class.java)
        method.isAccessible = true

        val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(promptingMain, userPrompt, invalidResponse)
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

        val method = promptingMain::class.java.getDeclaredMethod("prototypePrompt", String::class.java, JsonObject::class.java)
        method.isAccessible = true

        val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(promptingMain, userPrompt, invalidResponse)
        }

        assertTrue(exception.cause is PromptException)
        assertEquals("Failed to extract keywords from LLM response", exception.cause?.message)
    }

    @Test
    fun `test promptLlm with successful response`() {
        val prompt = "test prompt"
        val expectedJson = buildJsonObject {
            put("test", JsonPrimitive("value"))
        }

        coEvery { 
            PrototypeInteractor.prompt(prompt, any()) 
        } returns OllamaResponse(
            model = "test-model",
            created_at = "2024-01-01",
            response = "test response",
            done = true,
            done_reason = "test"
        )

        every { PromptingTools.formatResponseJson("test response") } returns expectedJson

        val result = runBlocking {
            promptingMain::class.java.getDeclaredMethod("promptLlm", String::class.java)
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

        val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
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
        } returns OllamaResponse(
            model = "test-model",
            created_at = "2024-01-01",
            response = "invalid json",
            done = true,
            done_reason = "test"
        )

        every { PromptingTools.formatResponseJson("invalid json") } throws Exception("Invalid JSON")

        assertThrows<Exception> {
            runBlocking {
                promptingMain::class.java.getDeclaredMethod("promptLlm", String::class.java)
                    .apply { isAccessible = true }
                    .invoke(promptingMain, prompt)
            }
        }
    }

    @Test
    fun `test chatResponse with JsonArray`() {
        val response = buildJsonObject {
            put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
        }

        val method = promptingMain::class.java.getDeclaredMethod("chatResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ChatResponse

        assertEquals(
            "These are the functional requirements fulfilled by this prototype: req1, req2",
            result.response
        )
    }

    @Test
    fun `test chatResponse with JsonPrimitive`() {
        val response = buildJsonObject {
            put("requirements", JsonPrimitive("single requirement"))
        }

        val method = promptingMain::class.java.getDeclaredMethod("chatResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ChatResponse

        assertEquals(
            "These are the functional requirements fulfilled by this prototype: single requirement",
            result.response
        )
    }

    @Test
    fun `test chatResponse with empty JsonArray`() {
        val response = buildJsonObject {
            put("requirements", JsonArray(emptyList()))
        }

        val method = promptingMain::class.java.getDeclaredMethod("chatResponse", JsonObject::class.java)
        method.isAccessible = true

        val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(promptingMain, response)
        }

        assertTrue(exception.cause is PromptException)
        assertEquals("No requirements found in LLM response", exception.cause?.message)
    }

    @Test
    fun `test chatResponse with invalid type`() {
        val response = buildJsonObject {
            put("requirements", JsonObject(emptyMap()))
        }

        val method = promptingMain::class.java.getDeclaredMethod("chatResponse", JsonObject::class.java)
        method.isAccessible = true

        val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(promptingMain, response)
        }

        assertTrue(exception.cause is PromptException)
        assertEquals("Requirements could not be found or were returned in an unrecognised format.", exception.cause?.message)
    }

    @Test
    fun `test onSiteSecurityCheck with safe code`() {
        val llmResponse = LlmResponse(
            mainFile = "index.js",
            files = mapOf<String, FileContent>(
                "index.js" to FileContent("console.log('Hello')"),
                "style.css" to FileContent("body { color: black; }")
            )
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
        val llmResponse = LlmResponse(
            mainFile = "index.js",
            files = mapOf<String, FileContent>(
                "index.js" to FileContent("console.log('Hello')"),
                "malicious.js" to FileContent("eval('malicious code')")
            )
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

        val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
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
    fun `test webContainerResponse`() {
        // Test the empty function to ensure coverage
        val method = PromptingMain::class.java.getDeclaredMethod("webContainerResponse")
        method.isAccessible = true
        method.invoke(promptingMain)
    }
}
