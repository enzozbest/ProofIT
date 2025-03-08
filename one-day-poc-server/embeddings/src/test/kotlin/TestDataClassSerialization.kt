import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class TestDataClassSerialization {
    private val json = Json { prettyPrint = false }

    @Test
    fun `Test EmbeddingServiceResponse with values`() {
        val embeddingServiceResponse = TemplateEmbedResponse(status = "success", embedding = listOf(0.1f, 0.2f, 0.3f))
        assertEquals("success", embeddingServiceResponse.status)
        assertEquals(listOf(0.1f, 0.2f, 0.3f), embeddingServiceResponse.embedding)
    }

    @Test
    fun `Test EmbeddingServiceResponse serialisation with all values provided`() {
        val response = TemplateEmbedResponse(status = "success", embedding = listOf(0.1f, 0.2f, 0.3f))
        val encoded = json.encodeToString(response)
        val expectedJson = """{"status":"success","embedding":[0.1,0.2,0.3]}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateEmbedResponse>(encoded)
        assertEquals(response, decoded)
    }

    @Test
    fun `Test EmbeddingServiceResponse deserialization`() {
        val json = """{"status": "success","embedding":[1, 2, 3]}"""
        val expected = TemplateEmbedResponse(status = "success", embedding = listOf(1f, 2f, 3f))
        assertEquals(expected, Json.decodeFromString<TemplateEmbedResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test EmbeddingServiceResponse deserialization missing a field`() {
        val json = """{"embedding": [1,2,2]}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<TemplateEmbedResponse>(json)
        }
    }

    @Test
    fun `Test EmbeddingStoreResponse with values`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", message = "Success")
        assertEquals("success", embeddingStoreResponse.status)
        assertEquals("Success", embeddingStoreResponse.message)
    }

    @Test
    fun `Test EmbeddingStoreResponse with null`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", message = null)
        assertEquals("success", embeddingStoreResponse.status)
        assertNull(embeddingStoreResponse.message)
    }

    @Test
    fun `Test EmbeddingStoreResponse serialization with all values provided`() {
        val storeResponse = StoreTemplateResponse(status = "success", message = "Stored successfully")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","message":"Stored successfully"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        assertEquals(storeResponse, decoded)
    }

    @Test
    fun `Test EmbeddingStoreResponse serialisation with null`() {
        val storeResponse = StoreTemplateResponse(status = "error", message = null)
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"error"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        assertEquals(storeResponse, decoded)
        assertNull(decoded.message)
    }

    @Test
    fun `Test EmbeddingStoreResponse deserialization`() {
        val json = """{"status": "success","message": "Success"}"""
        val expected = StoreTemplateResponse(status = "success", message = "Success")
        assertEquals(expected, Json.decodeFromString<StoreTemplateResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test EmbeddingStoreResponse deserialization missing a field`() {
        val json = """{"message": "Success"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<StoreTemplateResponse>(json)
        }
    }

    @Test
    fun `Test SemanticSearchResponse with values`() {
        val semanticSearchResponse =
            TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        assertEquals("success", semanticSearchResponse.status)
        assertEquals(listOf("TemplateA", "TemplateB"), semanticSearchResponse.matches)
    }

    @Test
    fun `Test SemanticSearchResponse serialisation with matches`() {
        val searchResponse = TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success","matches":["TemplateA","TemplateB"]}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateSearchResponse>(encoded)
        assertEquals(searchResponse, decoded)
    }

    @Test
    fun `Test SemanticSearchResponses serialisation with empty matches`() {
        val searchResponse = TemplateSearchResponse(status = "success")
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateSearchResponse>(encoded)
        assertEquals(searchResponse, decoded)
        assertEquals(emptyList<String>(), decoded.matches)
    }

    @Test
    fun `Test SemanticSearchResponse deserialization`() {
        val json = """{"status": "success","matches": ["TemplateA", "TemplateB"]}"""
        val expected = TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        assertEquals(expected, Json.decodeFromString<TemplateSearchResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test SemanticSearchResponse deserialization missing a field`() {
        val json = """{"matches": ["TemplateA", "TemplateB"]}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<TemplateSearchResponse>(json)
        }
    }
}
