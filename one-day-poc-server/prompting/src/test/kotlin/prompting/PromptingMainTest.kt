package prompting

import io.mockk.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.helpers.OllamaService
import prototype.helpers.PromptException
import prototype.security.secureCodeCheck

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
                    sanitizedPrompt.keywords
            )
        } returns freqsPrompt

        every {
            PromptingTools.prototypePrompt(
                    userPrompt = userPrompt,
                    requirements = "req1 req2",
                    templates = listOf("key1", "key2"),
            )
        } returns prototypePrompt

        coEvery { OllamaService.generateResponse(any()) } returns
                Result.success(
                        OllamaResponse(
                                model = "test-model",
                                created_at = "2024-01-01",
                                response = "{\"key\": \"value\"}",
                                done = true,
                                done_reason = "test",
                        ),
                )

        coEvery { PrototypeInteractor.prompt(any(), any(), any()) } returns
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
                "Result should contain the expected message"
        )
    }

    @Test
    fun `test run when LLM returns null`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns "prompt"

        coEvery { PrototypeInteractor.prompt(any(), any(), any()) } returns null

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

        coEvery { PrototypeInteractor.prompt(eq("prompt"), any(), any()) } returns
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
    fun `test prototypePrompt with valid response`() {
        val userPrompt = "test prompt"
        val freqsResponse = buildJsonObject {
            put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
            put("keywords", JsonArray(listOf(JsonPrimitive("key1"), JsonPrimitive("key2"))))
        }

        val result =
                promptingMain::class
                        .java
                        .getDeclaredMethod(
                                "prototypePrompt",
                                String::class.java,
                                JsonObject::class.java,
                                List::class.java,
                                String::class.java,
                        )
                        .apply { isAccessible = true }
                        .invoke(
                                promptingMain,
                                userPrompt,
                                freqsResponse,
                                emptyList<String>(),
                                null
                        ) as
                        String

        assertTrue(result.contains(userPrompt), "Result should contain the user prompt")
        assertTrue(result.contains("req1"), "Result should contain the requirements")
        assertTrue(result.contains("req2"), "Result should contain the requirements")
        assertTrue(result.contains("templates"), "Result should contain the templates")
    }

    @Test
    fun `test prototypePrompt when requirements extraction fails`() {
        val userPrompt = "test prompt"
        val invalidResponse = buildJsonObject { put("wrong_key", JsonPrimitive("value")) }

        val method =
                promptingMain::class.java.getDeclaredMethod(
                        "prototypePrompt",
                        String::class.java,
                        JsonObject::class.java,
                        List::class.java,
                        String::class.java,
                )
        method.isAccessible = true

        val exception =
                assertThrows<java.lang.reflect.InvocationTargetException> {
                    method.invoke(
                            promptingMain,
                            userPrompt,
                            invalidResponse,
                            emptyList<String>(),
                            null
                    )
                }

        assertTrue(exception.cause is PromptException)
        assertEquals("Failed to extract requirements from LLM response", exception.cause?.message)
    }

    @Test
    fun `test prototypePrompt when keywords extraction fails`() {
        val userPrompt = "test prompt"
        val invalidResponse = buildJsonObject {
            put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
        }

        val method =
                promptingMain::class.java.getDeclaredMethod(
                        "prototypePrompt",
                        String::class.java,
                        JsonObject::class.java,
                        List::class.java,
                        String::class.java,
                )
        method.isAccessible = true

        val exception =
                assertThrows<java.lang.reflect.InvocationTargetException> {
                    method.invoke(
                            promptingMain,
                            userPrompt,
                            invalidResponse,
                            emptyList<String>(),
                            null
                    )
                }

        assertTrue(exception.cause is PromptException)
        assertEquals("Failed to extract keywords from LLM response", exception.cause?.message)
    }

    @Test
    fun `test prototypePrompt with default templates parameter`() {
        val userPrompt = "test prompt"
        val freqsResponse = buildJsonObject {
            put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
            put("keywords", JsonArray(listOf(JsonPrimitive("key1"), JsonPrimitive("key2"))))
        }

        val testPromptingMain = PromptingMain("test-model")

        val method =
                testPromptingMain::class.java.getDeclaredMethod(
                        "prototypePrompt",
                        String::class.java,
                        JsonObject::class.java,
                        List::class.java,
                        String::class.java,
                )
        method.isAccessible = true

        val testMethod = { userPrompt: String, freqsResponse: JsonObject ->
            method.invoke(
                    testPromptingMain,
                    userPrompt,
                    freqsResponse,
                    emptyList<String>(),
                    null
            ) as
                    String
        }

        mockkStatic(PromptingTools::class)
        every {
            PromptingTools.prototypePrompt(
                    userPrompt = any(),
                    requirements = any(),
                    templates = emptyList(),
            )
        } returns "mocked prototype prompt"

        val result = testMethod(userPrompt, freqsResponse)

        assertEquals("mocked prototype prompt", result)

        verify(exactly = 1) {
            PromptingTools.prototypePrompt(
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

        coEvery { PrototypeInteractor.prompt(prompt, any(), OllamaOptions()) } returns
                OllamaResponse(
                        model = "test-model",
                        created_at = "2024-01-01",
                        response = "test response",
                        done = true,
                        done_reason = "test",
                )

        every { PromptingTools.formatResponseJson("test response") } returns expectedJsonString

        val result = runBlocking {
            promptingMain::class
                    .java
                    .getDeclaredMethod("promptLlm", String::class.java, OllamaOptions::class.java)
                    .apply { isAccessible = true }
                    .invoke(promptingMain, prompt, OllamaOptions()) as
                    String
        }

        assertEquals(expectedJsonString, result)
    }

    @Test
    fun `test promptLlm when LLM returns null`() {
        val prompt = "test prompt"

        coEvery { PrototypeInteractor.prompt(prompt, any(), OllamaOptions()) } returns null

        val method =
                promptingMain::class.java.getDeclaredMethod(
                        "promptLlm",
                        String::class.java,
                        OllamaOptions::class.java
                )
        method.isAccessible = true

        val exception =
                assertThrows<java.lang.reflect.InvocationTargetException> {
                    runBlocking { method.invoke(promptingMain, prompt, OllamaOptions()) }
                }

        assertTrue(exception.cause is PromptException)
        assertEquals("LLM did not respond!", exception.cause?.message)
    }

    @Test
    fun `test promptLlm when JSON formatting fails`() {
        val prompt = "test prompt"

        coEvery { PrototypeInteractor.prompt(prompt, any(), OllamaOptions()) } returns
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
                                OllamaOptions::class.java
                        )
                        .apply { isAccessible = true }
                        .invoke(promptingMain, prompt, OllamaOptions())
            }
        }
    }

    @Test
    fun `test promptLlm with default options parameter`() {
        val prompt = "test prompt"
        val expectedJsonString = """{"test": "value"}"""

        val optionsSlot = slot<OllamaOptions>()

        coEvery { PrototypeInteractor.prompt(eq(prompt), any(), capture(optionsSlot)) } returns
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
                        OllamaOptions::class.java
                )
        method.isAccessible = true

        val result = runBlocking {
            method.invoke(testPromptingMain, prompt, OllamaOptions()) as String
        }

        assertEquals(expectedJsonString, result)

        coVerify(exactly = 1) { PrototypeInteractor.prompt(eq(prompt), eq("test-model"), any()) }
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
                PromptingMain::class.java.getDeclaredMethod(
                        "onSiteSecurityCheck",
                        LlmResponse::class.java
                )
        method.isAccessible = true
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
                                        "index.js" to FileContent("console.log('Hello')"),
                                        "malicious.js" to FileContent("eval('malicious code')"),
                                ),
                )

        mockkStatic(::secureCodeCheck)
        every { secureCodeCheck(eq("console.log('Hello')"), eq("index.js")) } returns true
        every { secureCodeCheck(eq("eval('malicious code')"), eq("malicious.js")) } returns false

        val method =
                PromptingMain::class.java.getDeclaredMethod(
                        "onSiteSecurityCheck",
                        LlmResponse::class.java
                )
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
        val sanitisedPrompt =
                SanitisedPromptResult("Create a hello world app", listOf("hello", "world"))
        val freqsPrompt = "Functional requirements prompt"
        val modelName = "qwen2.5-coder:14b"

        buildJsonObject {
            putJsonArray("requirements") { add("Display hello world") }
            putJsonArray("keywords") {
                add("hello")
                add("world")
            }
        }

        buildJsonObject { putJsonArray("requirements") { add("Display hello world") } }

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

        coEvery { PrototypeInteractor.prompt(any(), any(), any()) } returns
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
                    sanitisedPrompt.keywords
            )
            PromptingTools.formatResponseJson(any())
            PromptingTools.prototypePrompt(userPrompt, any(), any())
        }

        coVerify {
            PrototypeInteractor.prompt(freqsPrompt, modelName, any())
            TemplateInteractor.fetchTemplates(any())
            PrototypeInteractor.prompt(any(), modelName, any())
        }

        verify(atLeast = 1) { PromptingTools.prototypePrompt(userPrompt, any(), any()) }

        assertTrue(result.contains("Display hello world"), "Result should contain the requirements")

        unmockkObject(SanitisationTools)
        unmockkObject(PromptingTools)
        unmockkObject(PrototypeInteractor)
        unmockkObject(TemplateInteractor)
    }

    /**
     * Helper method to parse a JsonObject into a structure similar to what the original
     * serverResponse() method returned. This is used for testing purposes only.
     */
    private data class TestServerResponse(
            val chat: ChatResponse,
            val prototype: PrototypeResponse?
    )

    private fun parseServerResponse(response: JsonObject): TestServerResponse {
        val chatMessage =
                when (val chatValue = response["chat"]) {
                    is JsonPrimitive -> chatValue.content
                    is JsonObject -> {
                        val messageField = chatValue["message"]
                        if (messageField == null) {
                            "Here is your code."
                        } else if (messageField !is JsonPrimitive) {
                            throw IllegalArgumentException("Message field is not a JsonPrimitive")
                        } else {
                            messageField.content
                        }
                    }
                    else -> "Here is your code."
                }

        val chatResponse =
                ChatResponse(
                        message = chatMessage,
                        role = "LLM",
                        timestamp = "2024-01-01T00:00:00Z",
                        messageId = "0"
                )

        val prototypeResponse =
                when (val prototypeValue = response["prototype"]) {
                    is JsonObject -> {
                        val filesField = prototypeValue["files"]
                        if (filesField is JsonObject) {
                            PrototypeResponse(files = filesField.toString())
                        } else {
                            null
                        }
                    }
                    else -> null
                }

        return TestServerResponse(chatResponse, prototypeResponse)
    }

    @Test
    fun `test parseServerResponse with chat as JsonObject with message field`() {
        val response = buildJsonObject {
            putJsonObject("chat") { put("message", JsonPrimitive("Custom message from LLM")) }
        }

        val result = parseServerResponse(response)

        assertEquals(
                "Custom message from LLM",
                result.chat.message,
        )
        assertEquals("LLM", result.chat.role)
        assertEquals("0", result.chat.messageId)
    }

    @Test
    fun `test parseServerResponse with prototype as JsonObject with files field`() {
        val filesObject = buildJsonObject {
            putJsonObject("file1.js") { put("content", JsonPrimitive("console.log('Hello');")) }
        }

        val response = buildJsonObject {
            put("chat", JsonPrimitive("Chat message"))
            putJsonObject("prototype") { put("files", filesObject) }
        }

        val result = parseServerResponse(response)

        assertEquals("Chat message", result.chat.message)
        assertTrue(
                result.prototype?.files?.contains("console.log('Hello')") == true,
                "Files should contain the expected content",
        )
    }

    @Test
    fun `test parseServerResponse with prototype as JsonObject without files field`() {
        val response = buildJsonObject {
            put("chat", JsonPrimitive("Chat message"))
            putJsonObject("prototype") { put("someOtherField", JsonPrimitive("value")) }
        }

        val result = parseServerResponse(response)

        assertEquals("Chat message", result.chat.message)
        assertEquals(null, result.prototype)
    }

    @Test
    fun `test parseServerResponse with prototype not as JsonObject`() {
        val response = buildJsonObject {
            put("chat", JsonPrimitive("Chat message"))
            putJsonArray("prototype") { add(JsonPrimitive("Not a JsonObject")) }
        }

        val result = parseServerResponse(response)

        assertEquals("Chat message", result.chat.message)
        assertEquals(null, result.prototype)
    }

    @Test
    fun `test parseServerResponse with chat as JsonObject without message field`() {
        val response = buildJsonObject {
            putJsonObject("chat") { put("otherField", JsonPrimitive("Some value")) }
        }

        val result = parseServerResponse(response)

        assertEquals("Here is your code.", result.chat.message)
        assertEquals("LLM", result.chat.role)
        assertEquals("0", result.chat.messageId)
    }

    @Test
    fun `test parseServerResponse with chat as JsonObject with message field not as JsonPrimitive`() {
        val response = buildJsonObject {
            putJsonObject("chat") {
                putJsonObject("message") { put("nestedField", JsonPrimitive("Nested value")) }
            }
        }

        val exception = assertThrows<IllegalArgumentException> { parseServerResponse(response) }

        assertTrue(exception.message?.contains("is not a JsonPrimitive") == true)
    }

    @Test
    fun `test promptLlm when llmResponse response is null`() {
        val prompt = "test prompt"

        coEvery { PrototypeInteractor.prompt(prompt, any(), OllamaOptions()) } returns
                OllamaResponse(
                        model = "test-model",
                        created_at = "2024-01-01",
                        response = null,
                        done = true,
                        done_reason = "test"
                )

        val method =
                promptingMain::class.java.getDeclaredMethod(
                        "promptLlm",
                        String::class.java,
                        OllamaOptions::class.java
                )
        method.isAccessible = true

        val exception =
                assertThrows<java.lang.reflect.InvocationTargetException> {
                    runBlocking { method.invoke(promptingMain, prompt, OllamaOptions()) }
                }

        assertTrue(exception.cause is PromptException)
        assertEquals("LLM response was null!", exception.cause?.message)

        coVerify(exactly = 1) { PrototypeInteractor.prompt(prompt, any(), OllamaOptions()) }
        verify(exactly = 0) { PromptingTools.formatResponseJson(any()) }
    }

    @Test
    fun `test JSON parsing with error handling in run method`() {
        val userPrompt = "test prompt"
        val sanitizedPrompt = SanitisedPromptResult("sanitized test prompt", listOf("keyword1"))
        val freqsPrompt = "functional requirements prompt"

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitizedPrompt
        every { PromptingTools.functionalRequirementsPrompt(any(), any()) } returns freqsPrompt

        coEvery { PrototypeInteractor.prompt(eq(freqsPrompt), any(), any()) } returns
                OllamaResponse(
                        model = "test-model",
                        created_at = "2024-01-01",
                        response = "malformed response",
                        done = true,
                        done_reason = "test"
                )

        every { PromptingTools.formatResponseJson("malformed response") } returns
                "{ this is not valid JSON }"

        val fetcherInputSlot = slot<String>()
        coEvery { TemplateInteractor.fetchTemplates(capture(fetcherInputSlot)) } returns emptyList()

        val exception =
                assertThrows<PromptException> { runBlocking { promptingMain.run(userPrompt) } }

        assertEquals("Failed to extract requirements from LLM response", exception.message)

        assertEquals(
                ", $userPrompt",
                fetcherInputSlot.captured,
                "With invalid JSON, requirements should be empty and only userPrompt should be used"
        )

        verify { PromptingTools.formatResponseJson("malformed response") }

        coVerify {
            PrototypeInteractor.prompt(eq(freqsPrompt), any(), any())
            TemplateInteractor.fetchTemplates(any())
        }
    }

    @Test
    fun `test keywords extraction in prototypePrompt with error handling`() {
        val userPrompt = "test prompt"

        mockkStatic(PromptingTools::class)

        val templatesSlot = slot<List<String>>()

        every {
            PromptingTools.prototypePrompt(
                    userPrompt = any(),
                    requirements = any(),
                    templates = capture(templatesSlot),
                    previousGeneration = any()
            )
        } returns "mocked prototype prompt"

        val method =
                promptingMain::class.java.getDeclaredMethod(
                        "prototypePrompt",
                        String::class.java,
                        JsonObject::class.java,
                        List::class.java,
                        String::class.java
                )
        method.isAccessible = true

        run {
            val validResponse = buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
                put("keywords", JsonArray(listOf(JsonPrimitive("key1"), JsonPrimitive("key2"))))
            }

            method.invoke(promptingMain, userPrompt, validResponse, emptyList<String>(), null)

            verify {
                PromptingTools.prototypePrompt(
                        userPrompt = userPrompt,
                        requirements = any(),
                        templates =
                                emptyList(),
                        previousGeneration = null
                )
            }
        }

        run {
            clearMocks(PromptingTools)
            every {
                PromptingTools.prototypePrompt(
                        userPrompt = any(),
                        requirements = any(),
                        templates = capture(templatesSlot),
                        previousGeneration = any()
                )
            } returns "mocked prototype prompt"

            val responseWithStringKeywords = buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
                put("keywords", JsonPrimitive("not an array"))
            }

            method.invoke(
                    promptingMain,
                    userPrompt,
                    responseWithStringKeywords,
                    emptyList<String>(),
                    null
            )

            verify {
                PromptingTools.prototypePrompt(
                        userPrompt = userPrompt,
                        requirements = any(),
                        templates = emptyList(),
                        previousGeneration = null
                )
            }
        }

        unmockkStatic(PromptingTools::class)
    }
}
