import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class TestEmbeddingService {
    private val embedResponseSuccessJson =
        """
        {"status":"success", "embedding":"0.1,0.2,0.3"}
        """.trimIndent()

    private val embedStoreResponseSuccessJson =
        """
        {"status":"success", "message":"Embedding stored successfully"}
        """.trimIndent()

    private val semanticSearchResponseSuccessJson =
        """
        {"status":"success", "matches":["TemplateA", "TemplateB"]}
        """.trimIndent()

    @Test
    fun `Test embed returns correct response on success`() =
        runBlocking {
            val engine =
                MockEngine { request ->
                    when (request.url.toString()) {
                        EmbeddingConstants.EMBED_URL ->
                            respond(
                                content = embedResponseSuccessJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                            )

                        else -> error("Unhandled ${request.url}")
                    }
                }

            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            val response = EmbeddingService.embed("Test text", "Test name")
            assertEquals("success", response.status)
            val floats = response.embedding?.split(",")?.map { it.toFloat() } ?: emptyList()
            assertEquals(listOf(0.1f, 0.2f, 0.3f), floats)
        }

    @Test
    fun `Test embed throws exception if response is not formatted correctly`(): Unit =
        runBlocking {
            val engine =
                MockEngine { request ->
                    respond(
                        content = "invalid json",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            assertFailsWith<IllegalStateException> {
                EmbeddingService.embed("Test text", "Test name")
            }
        }

    @Test
    fun `Test embed throws exception if response is empty`(): Unit =
        runBlocking {
            val engine =
                MockEngine { request ->
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            assertFailsWith<IllegalStateException> {
                EmbeddingService.embed("Test text", "Test name")
            }
        }

    @Test
    fun `Test embedAndStore returns expected response on success`() =
        runBlocking {
            val engine =
                MockEngine { request ->
                    when (request.url.toString()) {
                        EmbeddingConstants.EMBED_AND_STORE_URL ->
                            respond(
                                content = embedStoreResponseSuccessJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                            )

                        else -> error("Unhandled ${request.url}")
                    }
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            val response = EmbeddingService.embedAndStore("Test name", "Test text")
            assertEquals("success", response.status)
        }

    @Test
    fun `Test embedAndStore throws exception when response is not formatted correctly`(): Unit =
        runBlocking {
            val engine =
                MockEngine { request ->
                    respond(
                        content = "invalid json",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            assertFailsWith<IllegalStateException> {
                EmbeddingService.embedAndStore("Test name", "Test text")
            }
        }

    @Test
    fun `Test embedAndStore throws exception when response is empty`(): Unit =
        runBlocking {
            val engine =
                MockEngine { request ->
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            assertFailsWith<IllegalStateException> {
                EmbeddingService.embedAndStore("Test name", "Test text")
            }
        }

    @Test
    fun `Test semanticSearch returns expected response on success`() =
        runBlocking {
            val engine =
                MockEngine { request ->
                    when (request.url.toString()) {
                        EmbeddingConstants.SEMANTIC_SEARCH_URL ->
                            respond(
                                content = semanticSearchResponseSuccessJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                            )

                        else -> error("Unhandled ${request.url}")
                    }
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            val response = EmbeddingService.semanticSearch(listOf(0.1f, 0.2f, 0.3f))
            assertEquals("success", response.status)
            assertEquals(listOf("TemplateA", "TemplateB"), response.matches)
        }

    @Test
    fun `Test semanticSearch throws exception if response is not formatted correctly`(): Unit =
        runBlocking {
            val engine =
                MockEngine { request ->
                    respond(
                        content = "invalid json",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            assertFailsWith<IllegalStateException> {
                EmbeddingService.semanticSearch(listOf(0.1f, 0.2f, 0.3f))
            }
        }

    @Test
    fun `Test semanticSearch throws exception if response is empty`(): Unit =
        runBlocking {
            val engine =
                MockEngine { request ->
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            EmbeddingService.httpClient = client

            assertFailsWith<IllegalStateException> {
                EmbeddingService.semanticSearch(listOf(0.1f, 0.2f, 0.3f))
            }
        }
}
