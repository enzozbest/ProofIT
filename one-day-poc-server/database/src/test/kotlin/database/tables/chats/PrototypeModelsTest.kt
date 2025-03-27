package database.tables.chats

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PrototypeModelsTest {
    @Test
    fun `test default Prototype initialisation `(){
        val mId =UUID.randomUUID().toString()
        val prototype = Prototype(
            messageId = mId,
            filesJson = "json files",
        )
        assertNotNull(prototype.id)
        assertNotNull(prototype.timestamp)
        assertEquals(mId,prototype.messageId)
        assertEquals("json files", prototype.filesJson)
        assertEquals(1, prototype.version)
        assertEquals(true, prototype.isSelected)


    }
}