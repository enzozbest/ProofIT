import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kcl.seg.rtt.auth.AuthenticatedSession
import kcl.seg.rtt.auth.setUpJWTValidation
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestJWTValidationRoute {

    @Test
    fun `Test JWT validation route returns expected session details`() = testApplication {
        application {
            this@application.install(ContentNegotiation) {
                json()
            }
            this@application.routing {
                setUpJWTValidation("/validate")
            }
        }
        val session = AuthenticatedSession(userId = "user123", token = "tokenABC", admin = true)
        val cookieValue = Json.encodeToString<AuthenticatedSession>(session)

        val response = client.get("/validate") {
            cookie("AuthenticatedSession", cookieValue)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()

        val expectedJson = Json.parseToJsonElement("""{"userId":"user123","admin":true}""")
        val actualJson = Json.parseToJsonElement(responseBody)
        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `Test JWT validation route returns empty response without cookie`() = testApplication {
        application {
            this@application.install(ContentNegotiation) {
                json()
            }
            this@application.routing {
                setUpJWTValidation("/validate")
            }
        }
        assertFailsWith<SerializationException> {
            client.get("/validate")
        }
    }

}