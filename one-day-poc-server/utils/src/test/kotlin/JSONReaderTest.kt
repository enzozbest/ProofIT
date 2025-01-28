import kcl.seg.rtt.utils.JSON.readJsonFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JSONReaderTest {

    @Test
    fun testJSONReader() {
        val json = readJsonFile("src/test/resources/test.json")
        assertEquals("value", json.getString("key"))
    }

}