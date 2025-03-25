package prompting.helpers

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prompting.helpers.promptEngineering.Response
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResponseHandlerTest {

    private lateinit var mockHttpResponse: HttpResponse
    private lateinit var mockCall: ApplicationCall
    private lateinit var mockStatusCode: HttpStatusCode

    @BeforeEach
    fun setUp() {
        mockHttpResponse = mockk(relaxed = true)
        mockCall = mockk(relaxed = true)
        mockStatusCode = mockk(relaxed = true)

        every { mockHttpResponse.status } returns mockStatusCode

        mockkStatic("io.ktor.client.statement.HttpResponseKt")
        mockkStatic("io.ktor.http.HttpStatusCodeKt")
        mockkStatic("io.ktor.server.response.ApplicationResponseFunctionsKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test createResponse formats message correctly`() = runBlocking {
        val message = "Test message"
        val currentTimePrefix = LocalDateTime.now().toString().substring(0, 10) // Get just the date part for comparison

        val response = ResponseHandler::class.java.getDeclaredMethod("createResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(ResponseHandler, message) as Response

        assertEquals(message, response.message)
        assertTrue(response.time.startsWith(currentTimePrefix), 
            "Expected time to start with current date: ${response.time}")
    }

    @Test
    fun `test handlePromptResponse calls handleSuccessResponse when status is success`() = runBlocking {
        every { mockStatusCode.isSuccess() } returns true
        coEvery { mockHttpResponse.bodyAsText() } returns "Success body"

        val responseSlot = slot<Response>()
        coEvery { mockCall.respond(capture(responseSlot)) } returns Unit

        ResponseHandler.handlePromptResponse(mockHttpResponse, mockCall)

        assertEquals("Success body", responseSlot.captured.message)
    }

    @Test
    fun `test handlePromptResponse calls handleFailureResponse when status is not success`() = runBlocking {
        every { mockStatusCode.isSuccess() } returns false
        coEvery { mockHttpResponse.bodyAsText() } returns "Error body"
        every { mockStatusCode.toString() } returns "500 Internal Server Error"

        val responseSlot = slot<Response>()
        coEvery { mockCall.respond(capture(responseSlot)) } returns Unit

        ResponseHandler.handlePromptResponse(mockHttpResponse, mockCall)

        assertEquals("Error: 500 Internal Server Error, Error body", responseSlot.captured.message)
    }

    @Test
    fun `test handleSuccessResponse creates response with body text`() = runBlocking {
        val responseBody = "Success response body"
        every { mockStatusCode.isSuccess() } returns true
        coEvery { mockHttpResponse.bodyAsText() } returns responseBody

        val responseSlot = slot<Response>()
        coEvery { mockCall.respond(capture(responseSlot)) } returns Unit

        ResponseHandler.handlePromptResponse(mockHttpResponse, mockCall)

        assertEquals(responseBody, responseSlot.captured.message)
    }

    @Test
    fun `test handleFailureResponse creates response with error message`() = runBlocking {
        val responseBody = "Error details"
        val statusMessage = "500 Internal Server Error"

        every { mockStatusCode.isSuccess() } returns false
        every { mockStatusCode.toString() } returns statusMessage
        coEvery { mockHttpResponse.bodyAsText() } returns responseBody

        val responseSlot = slot<Response>()
        coEvery { mockCall.respond(capture(responseSlot)) } returns Unit

        ResponseHandler.handlePromptResponse(mockHttpResponse, mockCall)

        val expectedErrorMessage = "Error: $statusMessage, $responseBody"
        assertEquals(expectedErrorMessage, responseSlot.captured.message)
    }
}
