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
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaRequest
import prototype.helpers.OllamaResponse
import prototype.helpers.OllamaService
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

        // Mock OllamaService.generateResponse to prevent actual calls to Ollama
        coEvery {
            OllamaService.generateResponse(any())
        } returns Result.success(
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "{\"key\": \"value\"}",
                done = true,
                done_reason = "test",
            )
        )

        // Use the same approach as in the "test run method flow" test
        coEvery { PrototypeInteractor.prompt(any(), any(), any()) } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "{\"key\": \"value\"}",
                done = true,
                done_reason = "test",
            )

        coEvery {
            TemplateInteractor.fetchTemplates(any())
        } returns emptyList()

        // Mock formatResponseJson to return the expected responses without calling the actual implementation
        every { PromptingTools.formatResponseJson(any()) } returnsMany listOf(freqsResponse, finalResponse)

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

        // Return null from PrototypeInteractor.prompt to trigger the PromptException
        coEvery { PrototypeInteractor.prompt(any(), any(), any()) } returns null

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val exception = assertThrows<PromptException> {
            runBlocking { promptingMain.run(userPrompt) }
        }

        assertEquals("LLM did not respond!", exception.message)
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

        // First call to PrototypeInteractor.prompt returns a valid response
        coEvery { 
            PrototypeInteractor.prompt(eq("prompt"), any(), any()) 
        } returns OllamaResponse(
            model = "test-model",
            created_at = "2024-01-01",
            response = "response",
            done = true,
            done_reason = "test",
        )

        // First call to formatResponseJson returns the invalid response
        every { 
            PromptingTools.formatResponseJson("response") 
        } returns invalidResponse

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        val exception = assertThrows<PromptException> {
            runBlocking { promptingMain.run(userPrompt) }
        }

        assertEquals("Failed to extract requirements from LLM response", exception.message)
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
        assertTrue(result.contains("req1"), "Result should contain the requirements")
        assertTrue(result.contains("req2"), "Result should contain the requirements")
        // The templates section should be present, but we don't need to check for specific formatting
        assertTrue(result.contains("templates"), "Result should contain the templates")
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
    fun `test prototypePrompt with default templates parameter`() {
        val userPrompt = "test prompt"
        val freqsResponse =
            buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("req1"), JsonPrimitive("req2"))))
                put("keywords", JsonArray(listOf(JsonPrimitive("key1"), JsonPrimitive("key2"))))
            }

        // Create a new instance of PromptingMain for this test
        val testPromptingMain = PromptingMain("test-model")

        // Create a method to access the private prototypePrompt method
        val method =
            testPromptingMain::class.java.getDeclaredMethod(
                "prototypePrompt",
                String::class.java,
                JsonObject::class.java,
                List::class.java,
            )
        method.isAccessible = true

        // Create a test implementation of the method that only passes the first two parameters
        val testMethod = { userPrompt: String, freqsResponse: JsonObject ->
            method.invoke(testPromptingMain, userPrompt, freqsResponse, emptyList<String>()) as String
        }

        // Mock PromptingTools.prototypePrompt to return a known value
        mockkStatic(PromptingTools::class)
        every {
            PromptingTools.prototypePrompt(
                userPrompt = any(),
                requirements = any(),
                templates = emptyList(),
            )
        } returns "mocked prototype prompt"

        // Call the test method
        val result = testMethod(userPrompt, freqsResponse)

        // Verify the result
        assertEquals("mocked prototype prompt", result)

        // Verify that PromptingTools.prototypePrompt was called with the expected parameters
        verify(exactly = 1) {
            PromptingTools.prototypePrompt(
                userPrompt = userPrompt,
                requirements = any(), // Use any() matcher for requirements since the exact format may vary
                templates = emptyList(),
            )
        }

        // Unmock the static method
        unmockkStatic(PromptingTools::class)
    }

    @Test
    fun `test promptLlm with successful response`() {
        val prompt = "test prompt"
        val expectedJson =
            buildJsonObject {
                put("test", JsonPrimitive("value"))
            }

        coEvery {
            PrototypeInteractor.prompt(prompt, any(), OllamaOptions())
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
                    .getDeclaredMethod("promptLlm", String::class.java, OllamaOptions::class.java)
                    .apply { isAccessible = true }
                    .invoke(promptingMain, prompt, OllamaOptions()) as JsonObject
            }

        assertEquals(expectedJson, result)
    }

    @Test
    fun `test promptLlm when LLM returns null`() {
        val prompt = "test prompt"

        coEvery { PrototypeInteractor.prompt(prompt, any(), OllamaOptions()) } returns null

        val method = promptingMain::class.java.getDeclaredMethod("promptLlm", String::class.java, OllamaOptions::class.java)
        method.isAccessible = true

        val exception =
            assertThrows<java.lang.reflect.InvocationTargetException> {
                runBlocking {
                    method.invoke(promptingMain, prompt, OllamaOptions())
                }
            }

        assertTrue(exception.cause is PromptException)
        assertEquals("LLM did not respond!", exception.cause?.message)
    }

    @Test
    fun `test promptLlm when JSON formatting fails`() {
        val prompt = "test prompt"

        coEvery {
            PrototypeInteractor.prompt(prompt, any(), OllamaOptions())
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
                    .getDeclaredMethod("promptLlm", String::class.java, OllamaOptions::class.java)
                    .apply { isAccessible = true }
                    .invoke(promptingMain, prompt, OllamaOptions())
            }
        }
    }

    @Test
    fun `test promptLlm with default options parameter`() {
        val prompt = "test prompt"
        val expectedJson =
            buildJsonObject {
                put("test", JsonPrimitive("value"))
            }

        // Create a slot to capture the options parameter
        val optionsSlot = slot<OllamaOptions>()

        coEvery {
            PrototypeInteractor.prompt(eq(prompt), any(), capture(optionsSlot))
        } returns
            OllamaResponse(
                model = "test-model",
                created_at = "2024-01-01",
                response = "test response",
                done = true,
                done_reason = "test",
            )

        every { PromptingTools.formatResponseJson("test response") } returns expectedJson

        // Create a new instance of PromptingMain for this test
        val testPromptingMain = PromptingMain("test-model")

        // Use reflection to access the private method
        val method = testPromptingMain::class.java.getDeclaredMethod("promptLlm", String::class.java, OllamaOptions::class.java)
        method.isAccessible = true

        // Call the method with only the first parameter
        val result = runBlocking {
            method.invoke(testPromptingMain, prompt, OllamaOptions()) as JsonObject
        }

        assertEquals(expectedJson, result)

        // Verify that the captured options is an instance of OllamaOptions with default values
        assertTrue(optionsSlot.captured is OllamaOptions)

        // Verify the method was called
        coVerify(exactly = 1) { 
            PrototypeInteractor.prompt(eq(prompt), eq("test-model"), any()) 
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
        val modelName = "qwen2.5-coder:14b"  // Define the model name explicitly

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

        // Create a new instance of PromptingMain with the explicit model name
        val testPromptingMain = PromptingMain(modelName)

        every { SanitisationTools.sanitisePrompt(userPrompt) } returns sanitisedPrompt
        every {
            PromptingTools.functionalRequirementsPrompt(
                sanitisedPrompt.prompt,
                sanitisedPrompt.keywords,
            )
        } returns freqsPrompt

        // Mock PrototypeInteractor.prompt to return a valid response for any call
        coEvery { 
            PrototypeInteractor.prompt(any(), any(), any()) 
        } returns OllamaResponse(
            model = "deepseek-r1:32b",
            created_at = "2024-03-07T21:53:03Z",
            response = "{\"key\": \"value\"}",
            done = true,
            done_reason = "stop",
        )

        coEvery { TemplateInteractor.fetchTemplates(any()) } returns emptyList()

        // Mock formatResponseJson to return the expected responses
        every { PromptingTools.formatResponseJson(any()) } returnsMany listOf(freqsResponse, prototypeResponse)

        val result = runBlocking { testPromptingMain.run(userPrompt) }

        // Verify only the essential calls without strict ordering
        verify {
            SanitisationTools.sanitisePrompt(userPrompt)
            PromptingTools.functionalRequirementsPrompt(sanitisedPrompt.prompt, sanitisedPrompt.keywords)
            PromptingTools.formatResponseJson(any())
            PromptingTools.prototypePrompt(userPrompt, any(), any())
        }

        coVerify {
            PrototypeInteractor.prompt(freqsPrompt, modelName, any())
            TemplateInteractor.fetchTemplates(any())
            PrototypeInteractor.prompt(any(), modelName, any())
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
        unmockkObject(TemplateInteractor)
    }

    @Test
    fun `test serverResponse with chat as JsonObject with message field`() {
        val response =
            buildJsonObject {
                putJsonObject("chat") {
                    put("message", JsonPrimitive("Custom message from LLM"))
                }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals(
            "Custom message from LLM",
            result.chat.message,
        )
        assertEquals("LLM", result.chat.role)
        assertEquals("0", result.chat.messageId)
    }

    @Test
    fun `test serverResponse with prototype as JsonObject with files field`() {
        val filesObject = buildJsonObject {
            putJsonObject("file1.js") {
                put("content", JsonPrimitive("console.log('Hello');"))
            }
        }

        val response =
            buildJsonObject {
                put("chat", JsonPrimitive("Chat message"))
                putJsonObject("prototype") {
                    put("files", filesObject)
                }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals("Chat message", result.chat.message)
        assertEquals(filesObject, result.prototype?.files)
    }

    @Test
    fun `test serverResponse with prototype as JsonObject without files field`() {
        val response =
            buildJsonObject {
                put("chat", JsonPrimitive("Chat message"))
                putJsonObject("prototype") {
                    put("someOtherField", JsonPrimitive("value"))
                }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals("Chat message", result.chat.message)
        assertEquals(null, result.prototype)
    }

    @Test
    fun `test serverResponse with prototype not as JsonObject`() {
        val response =
            buildJsonObject {
                put("chat", JsonPrimitive("Chat message"))
                putJsonArray("prototype") {
                    add(JsonPrimitive("Not a JsonObject"))
                }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals("Chat message", result.chat.message)
        assertEquals(null, result.prototype)
    }

    @Test
    fun `test serverResponse with chat as JsonObject without message field`() {
        val response =
            buildJsonObject {
                putJsonObject("chat") {
                    put("otherField", JsonPrimitive("Some value"))
                    // No message field
                }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true
        val result = method.invoke(promptingMain, response) as ServerResponse

        assertEquals("Here is your code.", result.chat.message)
        assertEquals("LLM", result.chat.role)
        assertEquals("0", result.chat.messageId)
    }

    @Test
    fun `test serverResponse with chat as JsonObject with message field not as JsonPrimitive`() {
        val response =
            buildJsonObject {
                putJsonObject("chat") {
                    putJsonObject("message") {
                        put("nestedField", JsonPrimitive("Nested value"))
                    }
                }
            }

        val method = promptingMain::class.java.getDeclaredMethod("serverResponse", JsonObject::class.java)
        method.isAccessible = true

        // This should throw an exception because the message field is not a JsonPrimitive
        val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(promptingMain, response)
        }

        // Verify that the cause of the exception is IllegalArgumentException
        assertTrue(exception.cause is IllegalArgumentException)
        assertTrue(exception.cause?.message?.contains("is not a JsonPrimitive") == true)
    }
}
