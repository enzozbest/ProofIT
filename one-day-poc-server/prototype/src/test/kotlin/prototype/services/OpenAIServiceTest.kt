package prototype.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prototype.helpers.OllamaOptions
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse
import utils.environment.EnvironmentLoader

class OpenAIServiceTest {
    private val validJsonResponse =
        """
        {
            "id": "test-id",
            "object": "test-object",
            "created_at": 1672531200,
            "status": "completed",
            "model": "gpt-4",
            "output": [
                {
                    "type": "text",
                    "id": "output-id",
                    "status": "completed",
                    "role": "assistant",
                    "content": [
                        {
                            "type": "text",
                            "text": "Test response"
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

    private val invalidJsonResponse =
        """
        { invalid json }
        """.trimIndent()

    private val emptyOutputResponse =
        """
        {
            "id": "test-id",
            "object": "test-object",
            "created_at": 1672531200,
            "status": "completed",
            "model": "gpt-4",
            "output": []
        }
        """.trimIndent()

    @BeforeEach
    fun setUp() {
        // Mock environment variables
        mockkObject(EnvironmentLoader)
        every { EnvironmentLoader.get("OPENAI_HOST") } returns "api.openai.com"
        every { EnvironmentLoader.get("OPENAI_PATH") } returns "/v1/chat/completions"
        every { EnvironmentLoader.get("OPENAI_API_KEY") } returns "test-api-key"
        every { EnvironmentLoader.get("OPENAI_ORGANISATION_ID") } returns "test-org-id"
        every { EnvironmentLoader.get("OPENAI_PROJECT_ID") } returns "test-project-id"
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        try {
            val clientField = OpenAIService.javaClass.getDeclaredField("client")
            clientField.isAccessible = true
            clientField.set(OpenAIService, null)
        } catch (_: Exception) {
        }
    }

    @Test
    fun `test generateResponse with valid options`() =
        runBlocking {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = ByteReadChannel(validJsonResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            setClient(HttpClient(mockEngine))
            val result =
                OpenAIService.generateResponse(
                    "Test prompt",
                    "gpt-4",
                    OpenAIOptions(temperature = 0.5),
                )
            assertTrue(result.isSuccess)
            val response = result.getOrNull()
            assertNotNull(response)
            assertTrue(response is OpenAIResponse)
            val openAIResponse = response as OpenAIResponse
            assertEquals("gpt-4", openAIResponse.model)
            assertEquals(1672531200, openAIResponse.createdAt)
            assertEquals("Test response", openAIResponse.response)
            assertTrue(openAIResponse.done)
            assertEquals("stop", openAIResponse.doneReason)
        }

    @Test
    fun `test generateResponse with invalid options type`() =
        runBlocking {
            val invalidOptions = OllamaOptions(temperature = 0.5)

            val result =
                OpenAIService.generateResponse(
                    "Test prompt",
                    "gpt-4",
                    invalidOptions,
                )

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
            assertEquals("Invalid options type for OpenAIService", exception?.message)
        }

    @Test
    fun `test generateResponse when API call throws exception`() =
        runBlocking {
            val mockEngine =
                MockEngine { _ ->
                    throw Exception("API call failed")
                }

            setClient(HttpClient(mockEngine))

            val result =
                OpenAIService.generateResponse(
                    "Test prompt",
                    "gpt-4",
                    OpenAIOptions(temperature = 0.5),
                )

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
            assertEquals("Failed to call OpenAI: API call failed", exception?.message)
        }

    @Test
    fun `test callOpenAI with successful response`() =
        runBlocking {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(validJsonResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            setClient(HttpClient(mockEngine))

            val requestBuilder =
                HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                }

            val response = OpenAIService.callOpenAI(requestBuilder, OpenAIOptions(temperature = 0.5))

            assertNotNull(response)
            assertEquals("gpt-4", response?.model)
            assertEquals(1672531200, response?.createdAt)
            assertEquals("Test response", response?.response)
            assertTrue(response?.done == true)
            assertEquals("stop", response?.doneReason)
        }

    @Test
    fun `test callOpenAI with invalid JSON response`() =
        runBlocking {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(invalidJsonResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            setClient(HttpClient(mockEngine))

            val requestBuilder =
                HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                }

            val response = OpenAIService.callOpenAI(requestBuilder, OpenAIOptions(temperature = 0.5))

            assertNull(response)
        }

    @Test
    fun `test callOpenAI with empty output response`() =
        runBlocking {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = ByteReadChannel(emptyOutputResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            setClient(HttpClient(mockEngine))

            val requestBuilder =
                HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                }

            val response = OpenAIService.callOpenAI(requestBuilder, OpenAIOptions(temperature = 0.5))

            assertNotNull(response)
            assertEquals("gpt-4", response?.model)
            assertEquals(1672531200, response?.createdAt)
            assertEquals("", response?.response)
            assertTrue(response?.done == true)
            assertEquals("stop", response?.doneReason)
        }

    @Test
    fun `test callOpenAI with HTTP error response`() =
        runBlocking {
            val mockEngine =
                MockEngine { _ ->
                    respondError(HttpStatusCode.InternalServerError)
                }

            setClient(HttpClient(mockEngine))

            val requestBuilder =
                HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                }

            val response = OpenAIService.callOpenAI(requestBuilder, OpenAIOptions(temperature = 0.5))

            assertNull(response)
        }

    @Test
    fun `test generateResponse with blank organisationId and projectId`() =
        runBlocking {
            unmockkObject(EnvironmentLoader)
            mockkObject(EnvironmentLoader)
            every { EnvironmentLoader.get("OPENAI_HOST") } returns "api.openai.com"
            every { EnvironmentLoader.get("OPENAI_PATH") } returns "/v1/chat/completions"
            every { EnvironmentLoader.get("OPENAI_API_KEY") } returns "test-api-key"
            every { EnvironmentLoader.get("OPENAI_ORGANISATION_ID") } returns ""
            every { EnvironmentLoader.get("OPENAI_PROJECT_ID") } returns ""

            val mockEngine =
                MockEngine { request ->
                    assertNull(request.headers["OpenAI-Organization"])
                    assertNull(request.headers["OpenAI-Project"])

                    respond(
                        content = ByteReadChannel(validJsonResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            setClient(HttpClient(mockEngine))

            val result =
                OpenAIService.generateResponse(
                    "Test prompt",
                    "gpt-4",
                    OpenAIOptions(temperature = 0.5),
                )

            assertTrue(result.isSuccess)
        }

    @Test
    fun `test generateInstructions returns expected JSON schema`() {
        val method = OpenAIService.javaClass.getDeclaredMethod("generateInstructions")
        method.isAccessible = true
        val instructions = method.invoke(OpenAIService) as String

        assertTrue(instructions.contains("\"format\": {"))
        assertTrue(instructions.contains("\"type\": \"json_schema\""))
        assertTrue(instructions.contains("\"name\": \"prototype_code\""))
        assertTrue(instructions.contains("\"schema\": {"))
        assertTrue(instructions.contains("\"title\": \"Ollama Response Schema\""))
        assertTrue(instructions.contains("\"properties\": {"))
        assertTrue(instructions.contains("\"chat\": {"))
        assertTrue(instructions.contains("\"prototype\": {"))
        assertTrue(instructions.contains("\"files\": {"))
        assertTrue(instructions.contains("\"strict\": true"))
    }

    @Test
    fun `Test generateResponse handles exception gracefully`() =
        runBlocking {
            mockkObject(OpenAIService)
            coEvery { OpenAIService.callOpenAI(any(), any()) } throws Exception("Test exception")

            val result =
                OpenAIService.generateResponse(
                    "Test prompt",
                    "gpt-4",
                    OpenAIOptions(temperature = 0.5),
                )

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
            assertEquals("Failed to call OpenAI: Test exception", exception?.message)
        }

    private fun setClient(client: HttpClient) {
        try {
            val clientField = OpenAIService.javaClass.getDeclaredField("client")
            clientField.isAccessible = true
            clientField.set(OpenAIService, client)
        } catch (e: Exception) {
            println("Could not set client field: ${e.message}")
        }
    }
}
