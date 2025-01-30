import kcl.seg.rtt.utils.JSON.PoCJSON
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JSONReaderTest {

    @Test
    fun testJSONReader() {
        val json = PoCJSON.readJsonFile("src/test/resources/test.json")
        assertEquals("value", json["key"]!!.jsonPrimitive.content)
    }

}