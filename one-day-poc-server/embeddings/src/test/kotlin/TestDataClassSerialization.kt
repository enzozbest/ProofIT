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
        val embeddingServiceResponse = EmbeddingServiceResponse(status = "success", embedding = "0.1,0.2,0.3")
        assertEquals("success", embeddingServiceResponse.status)
        assertEquals("0.1,0.2,0.3", embeddingServiceResponse.embedding)
    }

    @Test
    fun `Test EmbeddingServiceResponse with null`() {
        val embeddingServiceResponse = EmbeddingServiceResponse(status = "success", embedding = null)
        assertEquals("success", embeddingServiceResponse.status)
        assertNull(embeddingServiceResponse.embedding)
    }

    @Test
    fun `Test EmbeddingServiceResponse serialisation with all values provided`() {
        val response = EmbeddingServiceResponse(status = "success", embedding = "0.1,0.2,0.3")
        val encoded = json.encodeToString(response)
        val expectedJson = """{"status":"success","embedding":"0.1,0.2,0.3"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<EmbeddingServiceResponse>(encoded)
        assertEquals(response, decoded)
    }

    @Test
    fun `Test EmbeddingServiceResponse serialisation with null`() {
        val response = EmbeddingServiceResponse(status = "error", embedding = null)
        val encoded = json.encodeToString(response)
        val expectedJson = """{"status":"error"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<EmbeddingServiceResponse>(encoded)
        assertEquals(response, decoded)
        assertNull(decoded.embedding)
    }

    @Test
    fun `Test EmbeddingServiceResponse deserialization`() {
        val json = """{"status": "success","embedding": "1f,2f,2f"}"""
        val expected = EmbeddingServiceResponse(status = "success", embedding = "1f,2f,2f")
        assertEquals(expected, Json.decodeFromString<EmbeddingServiceResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test EmbeddingServiceResponse deserialization missing a field`() {
        val json = """{"embedding": "1f,2f,2f"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<EmbeddingServiceResponse>(json)
        }
    }

    @Test
    fun `Test EmbeddingStoreResponse with values`() {
        val embeddingStoreResponse = EmbeddingStoreResponse(status = "success", message = "Success")
        assertEquals("success", embeddingStoreResponse.status)
        assertEquals("Success", embeddingStoreResponse.message)
    }

    @Test
    fun `Test EmbeddingStoreResponse with null`() {
        val embeddingStoreResponse = EmbeddingStoreResponse(status = "success", message = null)
        assertEquals("success", embeddingStoreResponse.status)
        assertNull(embeddingStoreResponse.message)
    }

    @Test
    fun `Test EmbeddingStoreResponse serialization with all values provided`() {
        val storeResponse = EmbeddingStoreResponse(status = "success", message = "Stored successfully")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","message":"Stored successfully"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<EmbeddingStoreResponse>(encoded)
        assertEquals(storeResponse, decoded)
    }

    @Test
    fun `Test EmbeddingStoreResponse serialisation with null`() {
        val storeResponse = EmbeddingStoreResponse(status = "error", message = null)
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"error"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<EmbeddingStoreResponse>(encoded)
        assertEquals(storeResponse, decoded)
        assertNull(decoded.message)
    }

    @Test
    fun `Test EmbeddingStoreResponse deserialization`() {
        val json = """{"status": "success","message": "Success"}"""
        val expected = EmbeddingStoreResponse(status = "success", message = "Success")
        assertEquals(expected, Json.decodeFromString<EmbeddingStoreResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test EmbeddingStoreResponse deserialization missing a field`() {
        val json = """{"message": "Success"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<EmbeddingStoreResponse>(json)
        }
    }

    @Test
    fun `Test SemanticSearchResponse with values`() {
        val semanticSearchResponse =
            SemanticSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        assertEquals("success", semanticSearchResponse.status)
        assertEquals(listOf("TemplateA", "TemplateB"), semanticSearchResponse.matches)
    }

    @Test
    fun `Test SemanticSearchResponse serialisation with matches`() {
        val searchResponse = SemanticSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success","matches":["TemplateA","TemplateB"]}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<SemanticSearchResponse>(encoded)
        assertEquals(searchResponse, decoded)
    }

    @Test
    fun `Test SemanticSearchResponses serialisation with empty matches`() {
        val searchResponse = SemanticSearchResponse(status = "success")
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<SemanticSearchResponse>(encoded)
        assertEquals(searchResponse, decoded)
        assertEquals(emptyList<String>(), decoded.matches)
    }

    @Test
    fun `Test SemanticSearchResponse deserialization`() {
        val json = """{"status": "success","matches": ["TemplateA", "TemplateB"]}"""
        val expected = SemanticSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        assertEquals(expected, Json.decodeFromString<SemanticSearchResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test SemanticSearchResponse deserialization missing a field`() {
        val json = """{"matches": ["TemplateA", "TemplateB"]}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<SemanticSearchResponse>(json)
        }
    }
}
