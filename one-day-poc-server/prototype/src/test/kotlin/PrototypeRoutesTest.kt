import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import kcl.seg.rtt.prototype.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import org.mockito.kotlin.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.assertTrue

class PrototypeRoutesTest {
    private val mockPrototypeService = mock<PrototypeService>()

    private val testJson = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        client.get("/api/prototype/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("OK", bodyAsText())
        }
    }

    @Test
    fun testGeneratePrototype_Success() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        val mockResponse = LlmResponse(
            mainFile = "index.html",
            files = mapOf(
                "index.html" to FileContent("<html>Test content</html>")
            )
        )

        whenever(mockPrototypeService.generatePrototype(any()))
            .thenReturn(Result.success(mockResponse))

        client.post("/api/prototype/generate") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(GenerateRequest("Test prompt")))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(testJson.encodeToString(mockResponse), bodyAsText())
        }
    }

    @Test
    fun testGeneratePrototype_Failure() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        whenever(mockPrototypeService.generatePrototype(any()))
            .thenReturn(Result.failure(RuntimeException("Generation failed")))

        client.post("/api/prototype/generate") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(GenerateRequest("Test prompt")))
        }.apply {
            println(bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
            assertTrue(bodyAsText().contains("error"))
        }
    }

    @Test
    fun testGeneratePrototype_FailureNullMessage() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        // Create an exception with a null message
        val exceptionWithNullMessage = object : Exception() {
            override val message: String? = null
        }

        whenever(mockPrototypeService.generatePrototype(any()))
            .thenReturn(Result.failure(exceptionWithNullMessage))

        client.post("/api/prototype/generate") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(GenerateRequest("Test prompt")))
        }.apply {
            println(bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, status)
            assertTrue(bodyAsText().contains("error"))
        }
    }

    @Test
    fun testGeneratePrototype_InvalidRequest() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        client.post("/api/prototype/generate") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody("invalid json")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun testRetrievePrototype_Success() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        whenever(mockPrototypeService.retrievePrototype("123"))
            .thenReturn("<html>Prototype content</html>")

        client.get("/api/prototype/retrieve?id=123").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("<html>Prototype content</html>", bodyAsText())
            assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), contentType())
        }
    }

    @Test
    fun testRetrievePrototype_NotFound() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        whenever(mockPrototypeService.retrievePrototype("456"))
            .thenReturn(null)

        client.get("/api/prototype/retrieve?id=456").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals("No prototype found for ID: 456", bodyAsText())
        }
    }

    @Test
    fun testRetrievePrototype_MissingId() = testApplication {
        application {
            install(ContentNegotiation) {
                json(testJson)
            }
            prototypeRoutes(mockPrototypeService)
        }

        client.get("/api/prototype/retrieve").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("Prototype ID is missing.", bodyAsText())
        }
    }
}