package templates

import embeddings.EmbeddingConstants
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class TestTemplateService {
    private val embedResponseSuccessJson =
        """
        {"status":"success", "embedding":[0.1,0.2,0.3]}
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
            TemplateService.httpClient = client

            val response = TemplateService.embed("Test text", "Test name")
            assertEquals("success", response.status)
            val floats = response.embedding
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
            TemplateService.httpClient = client

            assertFailsWith<IllegalStateException> {
                TemplateService.embed("Test text", "Test name")
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
            TemplateService.httpClient = client

            assertFailsWith<IllegalStateException> {
                TemplateService.embed("Test text", "Test name")
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
            TemplateService.httpClient = client

            mockkObject(TemplateStorageService)
            coEvery {
                TemplateStorageService.createTemplate(any())
            } returns "mock-id"

            val response = TemplateService.storeTemplate("Test name", "file:///test/path", "Test text")
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
            TemplateService.httpClient = client

            assertFailsWith<IllegalStateException> {
                TemplateService.storeTemplate("Test name", "file:///test/path", "Test text")
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
            TemplateService.httpClient = client

            assertFailsWith<IllegalStateException> {
                TemplateService.storeTemplate("Test name", "file:///test/path", "Test text")
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
            TemplateService.httpClient = client

            val response = TemplateService.search(listOf(0.1f, 0.2f, 0.3f), "Test query")
            assertEquals("success", response.status)
            assertEquals(listOf("TemplateA", "TemplateB"), response.matches)
        }

    @Test
    fun `Test semanticSearch throws exception if response is not formatted correctly`(): Unit =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "invalid json",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            TemplateService.httpClient = client

            assertFailsWith<IllegalStateException> {
                TemplateService.search(listOf(0.1f, 0.2f, 0.3f), "Test query")
            }
        }

    @Test
    fun `Test semanticSearch throws exception if response is empty`(): Unit =
        runBlocking {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                }
            val client = HttpClient(engine)
            TemplateService.httpClient = client

            assertFailsWith<IllegalStateException> {
                TemplateService.search(listOf(0.1f, 0.2f, 0.3f), "Test query")
            }
        }

    @Test // For coverage only!
    fun `Test getHttpClient$embeddings function is called`() {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    )
                },
            )
        TemplateService.httpClient = client
        val method = TemplateService::class.java.getDeclaredMethod("getHttpClient\$embeddings")
        method.isAccessible = true
        val retrievedClient = method.invoke(TemplateService)
        assertEquals(client, retrievedClient)
    }
}
