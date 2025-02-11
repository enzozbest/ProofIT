import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kcl.seg.rtt.auth.authentication.AuthenticatedSession
import kcl.seg.rtt.auth.authentication.setUpJWTValidation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

class TestJWTValidationRoute {
    @Test
    fun `Test JWT validation route returns expected session details`() =
        testApplication {
            application {
                this@application.install(ContentNegotiation) {
                    json()
                }
                this@application.routing {
                    setUpJWTValidation("/validate")
                }
            }
            val jwt =
                JWT
                    .create()
                    .withClaim("sub", "user123")
                    .withClaim("admin", false)
                    .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                    .sign(Algorithm.none())
            val session = AuthenticatedSession(userId = "user123", token = jwt, admin = false)
            val cookieValue = Json.encodeToString<AuthenticatedSession>(session)

            val response =
                client.get("/validate") {
                    cookie("AuthenticatedSession", cookieValue)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            val expectedJson = Json.parseToJsonElement("""{"userId":"user123","admin":false}""")
            val actualJson = Json.parseToJsonElement(responseBody)
            assertEquals(expectedJson, actualJson)
        }

    @Test
    fun `Test JWT validation route returns empty response without cookie`() =
        testApplication {
            application {
                this@application.install(ContentNegotiation) {
                    json()
                }
                this@application.routing {
                    setUpJWTValidation("/validate")
                }
            }
            val response = client.get("/validate")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Invalid or missing credentials!", response.bodyAsText())
        }
}
