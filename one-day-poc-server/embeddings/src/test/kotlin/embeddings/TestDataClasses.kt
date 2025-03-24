package embeddings

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class TestDataClasses {
    private val json = Json { prettyPrint = false }

    @Test
    fun `Test TemplateEmbedResponse with values`() {
        val embeddingServiceResponse = TemplateEmbedResponse(status = "success", embedding = listOf(0.1f, 0.2f, 0.3f))
        Assertions.assertEquals("success", embeddingServiceResponse.status)
        Assertions.assertEquals(listOf(0.1f, 0.2f, 0.3f), embeddingServiceResponse.embedding)
    }

    @Test
    fun `Test TemplateEmbedResponse with default embedding parameter`() {
        val embeddingServiceResponse = TemplateEmbedResponse(status = "success")
        Assertions.assertEquals("success", embeddingServiceResponse.status)
        Assertions.assertEquals(emptyList<Float>(), embeddingServiceResponse.embedding)
    }

    @Test
    fun `Test TemplateEmbedResponse serialisation with all values provided`() {
        val response = TemplateEmbedResponse(status = "success", embedding = listOf(0.1f, 0.2f, 0.3f))
        val encoded = json.encodeToString(response)
        val expectedJson = """{"status":"success","embedding":[0.1,0.2,0.3]}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateEmbedResponse>(encoded)
        Assertions.assertEquals(response, decoded)
    }

    @Test
    fun `Test TemplateEmbedResponse deserialization`() {
        val json = """{"status": "success","embedding":[1, 2, 3]}"""
        val expected = TemplateEmbedResponse(status = "success", embedding = listOf(1f, 2f, 3f))
        Assertions.assertEquals(expected, Json.Default.decodeFromString<TemplateEmbedResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateEmbedResponse deserialization missing status field`() {
        val json = """{"embedding": [1,2,2]}"""
        assertFailsWith<MissingFieldException> {
            Json.Default.decodeFromString<TemplateEmbedResponse>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateEmbedResponse deserialization missing embedding field`() {
        val json = """{"status": "success"}"""
        Assertions.assertDoesNotThrow { Json.Default.decodeFromString<TemplateEmbedResponse>(json) }
        val deserialised = Json.Default.decodeFromString<TemplateEmbedResponse>(json)
        Assertions.assertEquals("success", deserialised.status)
        Assertions.assertEquals(emptyList<Float>(), deserialised.embedding)
    }

    @Test
    fun `Test StoreTemplateResponse with values`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", message = "Success")
        Assertions.assertEquals("success", embeddingStoreResponse.status)
        Assertions.assertEquals("Success", embeddingStoreResponse.message)
    }

    @Test
    fun `Test StoreTemplateResponse with null`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", message = null)
        Assertions.assertEquals("success", embeddingStoreResponse.status)
        Assertions.assertNull(embeddingStoreResponse.message)
        Assertions.assertNull(embeddingStoreResponse.id)
    }

    @Test
    fun `Test StoreTemplateResponse with id`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", id = "template-123")
        Assertions.assertEquals("success", embeddingStoreResponse.status)
        Assertions.assertEquals("template-123", embeddingStoreResponse.id)
        Assertions.assertNull(embeddingStoreResponse.message)
    }

    @Test
    fun `Test StoreTemplateResponse with id and message`() {
        val embeddingStoreResponse = StoreTemplateResponse(status = "success", id = "template-123", message = "Success")
        Assertions.assertEquals("success", embeddingStoreResponse.status)
        Assertions.assertEquals("template-123", embeddingStoreResponse.id)
        Assertions.assertEquals("Success", embeddingStoreResponse.message)
    }

    @Test
    fun `Test StoreTemplateResponse serialization with all values provided`() {
        val storeResponse = StoreTemplateResponse(status = "success", message = "Stored successfully")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","message":"Stored successfully"}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        Assertions.assertEquals(storeResponse, decoded)
    }

    @Test
    fun `Test StoreTemplateResponse serialisation with null`() {
        val storeResponse = StoreTemplateResponse(status = "error", message = null)
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"error"}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        Assertions.assertEquals(storeResponse, decoded)
        Assertions.assertNull(decoded.message)
        Assertions.assertNull(decoded.id)
    }

    @Test
    fun `Test StoreTemplateResponse serialisation with id`() {
        val storeResponse = StoreTemplateResponse(status = "success", id = "template-123")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","id":"template-123"}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        Assertions.assertEquals(storeResponse, decoded)
        Assertions.assertEquals("template-123", decoded.id)
        Assertions.assertNull(decoded.message)
    }

    @Test
    fun `Test StoreTemplateResponse serialisation with id and message`() {
        val storeResponse = StoreTemplateResponse(status = "success", id = "template-123", message = "Success")
        val encoded = json.encodeToString(storeResponse)
        val expectedJson = """{"status":"success","id":"template-123","message":"Success"}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<StoreTemplateResponse>(encoded)
        Assertions.assertEquals(storeResponse, decoded)
        Assertions.assertEquals("template-123", decoded.id)
        Assertions.assertEquals("Success", decoded.message)
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with message`() {
        val json = """{"status": "success","message": "Success"}"""
        val expected = StoreTemplateResponse(status = "success", message = "Success")
        Assertions.assertEquals(expected, Json.Default.decodeFromString<StoreTemplateResponse>(json))
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with status only`() {
        val json = """{"status": "success"}"""
        val expected = StoreTemplateResponse(status = "success")
        Assertions.assertEquals(expected, Json.Default.decodeFromString<StoreTemplateResponse>(json))
        Assertions.assertNull(Json.Default.decodeFromString<StoreTemplateResponse>(json).id)
        Assertions.assertNull(Json.Default.decodeFromString<StoreTemplateResponse>(json).message)
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with id`() {
        val json = """{"status": "success", "id": "template-123"}"""
        val expected = StoreTemplateResponse(status = "success", id = "template-123")
        Assertions.assertEquals(expected, Json.Default.decodeFromString<StoreTemplateResponse>(json))
        Assertions.assertNull(Json.Default.decodeFromString<StoreTemplateResponse>(json).message)
    }

    @Test
    fun `Test StoreTemplateResponse deserialization with id and message`() {
        val json = """{"status": "success", "id": "template-123", "message": "Success"}"""
        val expected = StoreTemplateResponse(status = "success", id = "template-123", message = "Success")
        Assertions.assertEquals(expected, Json.Default.decodeFromString<StoreTemplateResponse>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test StoreTemplateResponse deserialization missing status field`() {
        val json = """{"message": "Success", "id": "123"}"""
        assertFailsWith<MissingFieldException> {
            Json.Default.decodeFromString<StoreTemplateResponse>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test StoreTemplateResponse deserialization missing id field`() {
        val json = """{"message": "Success", "status": "success"}"""
        Assertions.assertDoesNotThrow { Json.Default.decodeFromString<StoreTemplateResponse>(json) }
        val deserialised = Json.Default.decodeFromString<StoreTemplateResponse>(json)
        Assertions.assertNull(deserialised.id)
        Assertions.assertEquals("Success", deserialised.message)
        Assertions.assertEquals("success", deserialised.status)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test StoreTemplateResponse deserialization missing message field`() {
        val json = """{"id": "123", "status": "success"}"""
        Assertions.assertDoesNotThrow { Json.Default.decodeFromString<StoreTemplateResponse>(json) }
        val deserialised = Json.Default.decodeFromString<StoreTemplateResponse>(json)
        Assertions.assertNull(deserialised.message)
        Assertions.assertEquals("123", deserialised.id)
        Assertions.assertEquals("success", deserialised.status)
    }

    @Test
    fun `Test TemplateSearchResponse with values`() {
        val semanticSearchResponse =
            TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        Assertions.assertEquals("success", semanticSearchResponse.status)
        Assertions.assertEquals(listOf("TemplateA", "TemplateB"), semanticSearchResponse.matches)
    }

    @Test
    fun `Test TemplateSearchResponse serialisation with matches`() {
        val searchResponse = TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success","matches":["TemplateA","TemplateB"]}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateSearchResponse>(encoded)
        Assertions.assertEquals(searchResponse, decoded)
    }

    @Test
    fun `Test TemplateSearchResponses serialisation with empty matches`() {
        val searchResponse = TemplateSearchResponse(status = "success")
        val encoded = json.encodeToString(searchResponse)
        val expectedJson = """{"status":"success"}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<TemplateSearchResponse>(encoded)
        Assertions.assertEquals(searchResponse, decoded)
        Assertions.assertEquals(emptyList<String>(), decoded.matches)
    }

    @Test
    fun `Test TemplateSearchResponse deserialization with matches`() {
        val json = """{"status": "success","matches": ["TemplateA", "TemplateB"]}"""
        val expected = TemplateSearchResponse(status = "success", matches = listOf("TemplateA", "TemplateB"))
        Assertions.assertEquals(expected, Json.Default.decodeFromString<TemplateSearchResponse>(json))
    }

    @Test
    fun `Test TemplateSearchResponse deserialization with status only`() {
        val json = """{"status": "success"}"""
        val expected = TemplateSearchResponse(status = "success")
        Assertions.assertEquals(expected, Json.Default.decodeFromString<TemplateSearchResponse>(json))
        Assertions.assertEquals(
            emptyList<String>(),
            Json.Default.decodeFromString<TemplateSearchResponse>(json).matches,
        )
    }

    @Test
    fun `Test TemplateSearchResponse deserialization with empty matches`() {
        val json = """{"status": "success", "matches": []}"""
        val expected = TemplateSearchResponse(status = "success", matches = emptyList())
        Assertions.assertEquals(expected, Json.Default.decodeFromString<TemplateSearchResponse>(json))
        Assertions.assertEquals(
            emptyList<String>(),
            Json.Default.decodeFromString<TemplateSearchResponse>(json).matches,
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateSearchResponse deserialization missing status field`() {
        val json = """{"matches": ["TemplateA", "TemplateB"]}"""
        assertFailsWith<MissingFieldException> {
            Json.Default.decodeFromString<TemplateSearchResponse>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test TemplateSearchResponse deserialization missing matches field`() {
        val json = """{"status":  "success"}"""
        Assertions.assertDoesNotThrow {
            Json.Default.decodeFromString<TemplateSearchResponse>(json)
        }
        val deserialised = Json.Default.decodeFromString<TemplateSearchResponse>(json)
        Assertions.assertEquals(emptyList<String>(), deserialised.matches)
        Assertions.assertEquals("success", deserialised.status)
    }

    @Test
    fun `Test SearchData with values`() {
        val searchData = SearchData(embedding = listOf(0.1f, 0.2f, 0.3f), query = "test query")
        Assertions.assertEquals(listOf(0.1f, 0.2f, 0.3f), searchData.embedding)
        Assertions.assertEquals("test query", searchData.query)
    }

    @Test
    fun `Test SearchData serialization`() {
        val searchData = SearchData(embedding = listOf(0.1f, 0.2f, 0.3f), query = "test query")
        val encoded = json.encodeToString(searchData)
        val expectedJson = """{"embedding":[0.1,0.2,0.3],"query":"test query"}"""
        Assertions.assertEquals(expectedJson, encoded)

        val decoded = json.decodeFromString<SearchData>(encoded)
        Assertions.assertEquals(searchData, decoded)
    }

    @Test
    fun `Test SearchData deserialization`() {
        val json = """{"embedding":[1,2,3],"query":"sample query"}"""
        val expected = SearchData(embedding = listOf(1f, 2f, 3f), query = "sample query")
        Assertions.assertEquals(expected, Json.Default.decodeFromString<SearchData>(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test SearchData deserialization missing embedding field`() {
        val json = """{"query":"sample query"}"""
        assertFailsWith<MissingFieldException> {
            Json.Default.decodeFromString<SearchData>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test SearchData deserialization missing query field`() {
        val json = """{"embedding":[1,2,3]}"""
        assertFailsWith<MissingFieldException> {
            Json.Default.decodeFromString<SearchData>(json)
        }
    }
}
