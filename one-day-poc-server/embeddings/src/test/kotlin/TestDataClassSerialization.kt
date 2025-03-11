import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertFailsWith

class TestDataClassSerialization {
    private val json = Json { prettyPrint = false }

    @Test
    fun `Test TemplateEmbedResponse with values`() {
        val embeddingServiceResponse = TemplateEmbedResponse(status = "success", embedding = listOf(0.1f, 0.2f, 0.3f))
        assertEquals("success", embeddingServiceResponse.status)
        assertEquals(listOf(0.1f, 0.2f, 0.3f), embeddingServiceResponse.embedding)
    }

    @Test
    fun `Test TemplateEmbedResponse with default embedding parameter`() {
        val embeddingServiceResponse = TemplateEmbedResponse(status = "success")
        assertEquals("success", embeddingServiceResponse.status)
        assertEquals(emptyList<Float>(), embeddingServiceResponse.embedding)
    }

    @Test
    fun `Test TemplateEmbedResponse serialisation with all values provided`() {
        val response = TemplateEmbedResponse(status = "success", embedding = listOf(0.1f, 0.2f, 0.3f))
        val encoded = json.encodeToString(response)
        val expectedJson = """{"status":"success","embedding":[0.1,0.2,0.3]}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateEmbedResponse>(encoded)
        assertEquals(response, decoded)
    }

    @Test
    fun `Test TemplateEmbedResponse deserialization`() {
        val json = """{"status": "success","embedding":[1, 2, 3]}"""
        val expected = TemplateEmbedResponse(status = "success", embedding = listOf(1f, 2f, 3f))
        assertEquals(expected, Json.decodeFromString<TemplateEmbedResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateEmbedResponse deserialization missing status field`() {
        val json = """{"embedding": [1,2,2]}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<TemplateEmbedResponse>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateEmbedResponse deserialization missing embedding field`() {
        val json = """{"status": "success"}"""
        assertDoesNotThrow { Json.decodeFromString<TemplateEmbedResponse>(json) }
        val deserialised = Json.decodeFromString<TemplateEmbedResponse>(json)
        assertEquals("success", deserialised.status)
        assertEquals(emptyList<Float>(), deserialised.embedding)
    }

    @Test
    fun `Test StoreTemplateResponse with values`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", message = "Success")
        assertEquals("success", embeddingStoreResponse.status)
        assertEquals("Success", embeddingStoreResponse.message)
    }

    @Test
    fun `Test StoreTemplateResponse with null`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", message = null)
        assertEquals("success", embeddingStoreResponse.status)
        assertNull(embeddingStoreResponse.message)
        assertNull(embeddingStoreResponse.id)
    }

    @Test
    fun `Test StoreTemplateResponse with id`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", id = "template-123")
        assertEquals("success", embeddingStoreResponse.status)
        assertEquals("template-123", embeddingStoreResponse.id)
        assertNull(embeddingStoreResponse.message)
    }

    @Test
    fun `Test StoreTemplateResponse with id and message`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", id = "template-123", message = "Success")
        assertEquals("success", embeddingStoreResponse.status)
        assertEquals("template-123", embeddingStoreResponse.id)
        assertEquals("Success", embeddingStoreResponse.message)
    }

    @Test
    fun `Test StoreTemplateResponse serialization with all values provided`() {
        val storeResponse = StoreTemplateResponse(status = "success", message = "Stored successfully")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","message":"Stored successfully"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        assertEquals(storeResponse, decoded)
    }

    @Test
    fun `Test StoreTemplateResponse serialisation with null`() {
        val storeResponse = StoreTemplateResponse(status = "error", message = null)
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"error"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        assertEquals(storeResponse, decoded)
        assertNull(decoded.message)
        assertNull(decoded.id)
    }

    @Test
    fun `Test StoreTemplateResponse serialisation with id`() {
        val storeResponse = StoreTemplateResponse(status = "success", id = "template-123")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","id":"template-123"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        assertEquals(storeResponse, decoded)
        assertEquals("template-123", decoded.id)
        assertNull(decoded.message)
    }

    @Test
    fun `Test StoreTemplateResponse serialisation with id and message`() {
        val storeResponse = StoreTemplateResponse(status = "success", id = "template-123", message = "Success")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","id":"template-123","message":"Success"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        assertEquals(storeResponse, decoded)
        assertEquals("template-123", decoded.id)
        assertEquals("Success", decoded.message)
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with message`() {
        val json = """{"status": "success","message": "Success"}"""
        val expected = StoreTemplateResponse(status = "success", message = "Success")
        assertEquals(expected, Json.decodeFromString<StoreTemplateResponse>(json))
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with status only`() {
        val json = """{"status": "success"}"""
        val expected = StoreTemplateResponse(status = "success")
        assertEquals(expected, Json.decodeFromString<StoreTemplateResponse>(json))
        assertNull(Json.decodeFromString<StoreTemplateResponse>(json).id)
        assertNull(Json.decodeFromString<StoreTemplateResponse>(json).message)
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with id`() {
        val json = """{"status": "success", "id": "template-123"}"""
        val expected = StoreTemplateResponse(status = "success", id = "template-123")
        assertEquals(expected, Json.decodeFromString<StoreTemplateResponse>(json))
        assertNull(Json.decodeFromString<StoreTemplateResponse>(json).message)
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with id and message`() {
        val json = """{"status": "success", "id": "template-123", "message": "Success"}"""
        val expected = StoreTemplateResponse(status = "success", id = "template-123", message = "Success")
        assertEquals(expected, Json.decodeFromString<StoreTemplateResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test StoreTemplateResponse deserialization missing status field`() {
        val json = """{"message": "Success", "id": "123"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<StoreTemplateResponse>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test StoreTemplateResponse deserialization missing id field`() {
        val json = """{"message": "Success", "status": "success"}"""
        assertDoesNotThrow { Json.decodeFromString<StoreTemplateResponse>(json) }
        val deserialised = Json.decodeFromString<StoreTemplateResponse>(json)
        assertNull(deserialised.id)
        assertEquals("Success", deserialised.message)
        assertEquals("success", deserialised.status)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test StoreTemplateResponse deserialization missing message field`() {
        val json = """{"id": "123", "status": "success"}"""
        assertDoesNotThrow { Json.decodeFromString<StoreTemplateResponse>(json) }
        val deserialised = Json.decodeFromString<StoreTemplateResponse>(json)
        assertNull(deserialised.message)
        assertEquals("123", deserialised.id)
        assertEquals("success", deserialised.status)
    }

    @Test
    fun `Test TemplateSearchResponse with values`() {
        val semanticSearchResponse =
            TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        assertEquals("success", semanticSearchResponse.status)
        assertEquals(listOf("TemplateA", "TemplateB"), semanticSearchResponse.matches)
    }

    @Test
    fun `Test TemplateSearchResponse serialisation with matches`() {
        val searchResponse = TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success","matches":["TemplateA","TemplateB"]}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateSearchResponse>(encoded)
        assertEquals(searchResponse, decoded)
    }

    @Test
    fun `Test TemplateSearchResponses serialisation with empty matches`() {
        val searchResponse = TemplateSearchResponse(status = "success")
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateSearchResponse>(encoded)
        assertEquals(searchResponse, decoded)
        assertEquals(emptyList<String>(), decoded.matches)
    }

    @Test
    fun `Test TemplateSearchResponse deserialization with matches`() {
        val json = """{"status": "success","matches": ["TemplateA", "TemplateB"]}"""
        val expected = TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        assertEquals(expected, Json.decodeFromString<TemplateSearchResponse>(json))
    }

    @Test
    fun `Test TemplateSearchResponse deserialization with status only`() {
        val json = """{"status": "success"}"""
        val expected = TemplateSearchResponse(status = "success")
        assertEquals(expected, Json.decodeFromString<TemplateSearchResponse>(json))
        assertEquals(emptyList<String>(), Json.decodeFromString<TemplateSearchResponse>(json).matches)
    }

    @Test
    fun `Test TemplateSearchResponse deserialization with empty matches`() {
        val json = """{"status": "success", "matches": []}"""
        val expected = TemplateSearchResponse(status = "success", matches = emptyList())
        assertEquals(expected, Json.decodeFromString<TemplateSearchResponse>(json))
        assertEquals(emptyList<String>(), Json.decodeFromString<TemplateSearchResponse>(json).matches)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateSearchResponse deserialization missing status field`() {
        val json = """{"matches": ["TemplateA", "TemplateB"]}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<TemplateSearchResponse>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateSearchResponse deserialization missing matches field`() {
        val json = """{"status":  "success"}"""
        assertDoesNotThrow {
            Json.decodeFromString<TemplateSearchResponse>(json)
        }
        val deserialised = Json.decodeFromString<TemplateSearchResponse>(json)
        assertEquals(emptyList<String>(), deserialised.matches)
        assertEquals("success", deserialised.status)
    }

    @Test
    fun `Test SearchData with values`() {
        val searchData = SearchData(embedding = listOf(0.1f, 0.2f, 0.3f), query = "test query")
        assertEquals(listOf(0.1f, 0.2f, 0.3f), searchData.embedding)
        assertEquals("test query", searchData.query)
    }

    @Test
    fun `Test SearchData serialization`() {
        val searchData = SearchData(embedding = listOf(0.1f, 0.2f, 0.3f), query = "test query")
        val encoded = json.encodeToString(searchData)
        val expectedJson = """{"embedding":[0.1,0.2,0.3],"query":"test query"}"""
        assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<SearchData>(encoded)
        assertEquals(searchData, decoded)
    }

    @Test
    fun `Test SearchData deserialization`() {
        val json = """{"embedding":[1,2,3],"query":"sample query"}"""
        val expected = SearchData(embedding = listOf(1f, 2f, 3f), query = "sample query")
        assertEquals(expected, Json.decodeFromString<SearchData>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test SearchData deserialization missing embedding field`() {
        val json = """{"query":"sample query"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<SearchData>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test SearchData deserialization missing query field`() {
        val json = """{"embedding":[1,2,3]}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<SearchData>(json)
        }
    }
}
