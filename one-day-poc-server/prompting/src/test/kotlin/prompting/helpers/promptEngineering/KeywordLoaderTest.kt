package prompting.helpers.promptEngineering

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KeywordLoaderTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test KeywordList serialization`() {
        val keywordList = KeywordList(listOf("test1", "test2"))
        val serialized = Json.encodeToString(KeywordList.serializer(), keywordList)
        assertEquals("""{"keywords":["test1","test2"]}""", serialized)
    }

    @Test
    fun `test KeywordList deserialization`() {
        val json = """{"keywords":["test1","test2"]}"""
        val deserialized = Json.decodeFromString<KeywordList>(json)
        assertContentEquals(listOf("test1", "test2"), deserialized.keywords)
    }

    @Test
    fun `test KeywordList deserialization with empty list`() {
        val json = """{"keywords":[]}"""
        val deserialized = Json.decodeFromString<KeywordList>(json)
        assertTrue(deserialized.keywords.isEmpty())
    }

    @Test
    fun `test KeywordList deserialization with special characters`() {
        val json = """{"keywords":["test!@#","${'$'}%^&*"]}"""
        val deserialized = Json.decodeFromString<KeywordList>(json)
        assertContentEquals(listOf("test!@#", "${'$'}%^&*"), deserialized.keywords)
    }

    @Test
    fun `test KeywordList deserialization with invalid JSON throws exception`() {
        val invalidJson = """{"keywords": "not_an_array"}"""
        assertThrows<SerializationException> {
            Json.decodeFromString<KeywordList>(invalidJson)
        }
    }

    @Test
    fun `test KeywordList deserialization with missing field throws exception`() {
        val invalidJson = """{"wrong_field": []}"""
        assertThrows<SerializationException> {
            Json.decodeFromString<KeywordList>(invalidJson)
        }
    }

    @Test
    fun `test KeywordList data class equality`() {
        val list1 = KeywordList(listOf("test1", "test2"))
        val list2 = KeywordList(listOf("test1", "test2"))
        val list3 = KeywordList(listOf("test2", "test1"))

        assertEquals(list1, list2, "Equal lists should be equal")
        assertTrue(list1 != list3, "Lists with different order should not be equal")
    }

    @Test
    fun `test toString representation of KeywordList`() {
        val keywordList = KeywordList(listOf("test1", "test2"))
        assertEquals("KeywordList(keywords=[test1, test2])", keywordList.toString())
    }

    @Test
    fun `test hashCode consistency`() {
        val list1 = KeywordList(listOf("test1", "test2"))
        val list2 = KeywordList(listOf("test1", "test2"))
        assertEquals(list1.hashCode(), list2.hashCode(), "Equal objects should have equal hash codes")
    }

    @Test
    fun `test KeywordLoader loads keywords from resource file`() {
        val keywords = KeywordLoader.getKeywordsList()
        assertContentEquals(
            listOf("test1", "test2", "test3"),
            keywords,
            "Keywords should match the content of the test resource file",
        )
    }

    @Test
    fun `test KeywordLoader returns same instance on multiple calls`() {
        val keywords1 = KeywordLoader.getKeywordsList()
        val keywords2 = KeywordLoader.getKeywordsList()
        assertTrue(keywords1 === keywords2, "Multiple calls should return the same list instance due to lazy loading")
    }

    @Test
    fun `test KeywordLoader list is unmodifiable`() {
        val keywords = KeywordLoader.getKeywordsList()
        assertThrows<UnsupportedOperationException> {
            (keywords as MutableList<String>).add("new")
        }
    }

    @Test
    fun `test deserialisation of KeywordList with missing field`() {
        val json =
            """{}
            """.trimMargin()

        assertFailsWith<SerializationException> {
            Json.decodeFromString<KeywordList>(json)
        }
    }

    @Test
    fun `test KeywordLoader returns an empty list when keywords file is not found`() {
        mockkObject(KeywordLoader)
        // Reset the keywords to force reinitialization
        KeywordLoader.resetKeywords()
        every { KeywordLoader.getResource(any()) } returns null

        val keywords = KeywordLoader.getKeywordsList()
        assertEquals(emptyList(), keywords)
    }
}
