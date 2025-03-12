package templates

import database.core.DatabaseManager
import database.tables.templates.TemplateRepository
import embeddings.EmbeddingConstants
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

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

    @BeforeEach
    fun setUp() {
        // No setup needed by default
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

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
    fun `Test embedAndStore returns expected response on success`() {
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

            // Setup DatabaseManager mock to allow createTemplate to work
            mockkObject(DatabaseManager)
            val mockRepo = mockk<TemplateRepository>()
            coEvery { DatabaseManager.templateRepository() } returns mockRepo
            coEvery {
                mockRepo.saveTemplateToDB(any())
            } returns Result.success(Unit)

            // Call the method that will use the real createTemplate function
            val response = TemplateService.storeTemplate("Test name", "file:///test/path", "Test text")

            // Verify the response
            assertEquals("success", response.status)
            assertNotNull(response.id)
        }
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
