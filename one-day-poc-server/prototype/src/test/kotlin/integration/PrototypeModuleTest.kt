import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kcl.seg.rtt.prototype.prototypeModule
import kotlin.test.Test
import kotlin.test.assertEquals


class PrototypeModuleTest {
    @Test
    fun `health check should return 200 OK`() = testApplication {
        application {
            prototypeModule()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"status\":\"OK\"}", response.bodyAsText())
    }
}