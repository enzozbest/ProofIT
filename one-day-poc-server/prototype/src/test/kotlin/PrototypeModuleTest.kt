import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class PrototypeModuleTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: HttpClient

    @BeforeEach
    fun setup() {
        // Initialize the MockWebServer and HttpClient
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = HttpClient(CIO)
    }

    @AfterEach
    fun tearDown() {
        // Shut down the mock server and the client
        mockWebServer.shutdown()
        client.close()
    }

    @Test
    fun `health check should return 200 OK`() = runBlocking {
        // Enqueue a mocked HTTP 200 OK response with the expected body
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("OK")
                .addHeader("Content-Type", "text/plain")
        )

        // Create the full URL using the MockWebServer's base URL and "/health" path
        val baseUrl = mockWebServer.url("/health").toString()

        // Send a GET request to the test server
        val response: HttpResponse = client.get(baseUrl)

        // Perform assertions
        assertEquals(HttpStatusCode.OK, response.status, "Expected HTTP status 200 OK")
        assertEquals("OK", response.bodyAsText(), "Expected response body to be 'OK'")
    }
}