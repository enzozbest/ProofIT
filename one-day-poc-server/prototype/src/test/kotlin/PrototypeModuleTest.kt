import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kcl.seg.rtt.prototype.prototypeModule


class PrototypeRoutesTest {
    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            prototypeModule()
        }

        val response = client.get("api/prototype/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }
}

