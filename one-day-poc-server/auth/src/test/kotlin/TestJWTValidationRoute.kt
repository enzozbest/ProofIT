import authentication.authentication.AuthenticatedSession
import authentication.authentication.setUpJWTValidation
import helpers.AuthenticationTestHelpers
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestJWTValidationRoute {
    @Test
    fun `Test JWT validation route returns expected session details with cookie`() =
        testApplication {
            application {
                this@application.install(ContentNegotiation) {
                    json()
                }
                this@application.routing {
                    setUpJWTValidation("/validate")
                }
            }

            val session =
                AuthenticatedSession(
                    userId = "user123",
                    token = AuthenticationTestHelpers.generateTestJwtToken(),
                    admin = false,
                )
            val cookieValue = Json.encodeToString<AuthenticatedSession>(session)

            val response =
                client.get("/validate") {
                    cookie("AuthenticatedSession", cookieValue)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            val expectedJson = Json.parseToJsonElement("""{"userId":"user123","admin":true}""")
            val actualJson = Json.parseToJsonElement(responseBody)
            assertEquals(expectedJson, actualJson)
        }

    @Test
    fun `Test JWT validation route with invalid cookie`() =
        testApplication {
            application {
                this@application.install(ContentNegotiation) {
                    json()
                }
                this@application.routing {
                    setUpJWTValidation("/validate")
                }
            }

            val response =
                client.get("/validate") {
                    cookie("AuthenticatedSession", "invalid-cookie")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Invalid or missing credentials!", response.bodyAsText())
        }

    @Test
    fun `Test JWT validation route returns expected session details with header`() =
        testApplication {
            application {
                this@application.install(ContentNegotiation) {
                    json()
                }
                this@application.routing {
                    setUpJWTValidation("/validate")
                }
            }
            val response =
                client.get("/validate") {
                    header(HttpHeaders.Authorization, "Bearer ${AuthenticationTestHelpers.generateTestJwtToken()}")
                }
            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = response.bodyAsText()
            val expectedJson = Json.parseToJsonElement("""{"userId":"user123","admin":true}""")
            val actualJson = Json.parseToJsonElement(responseBody)
            assertEquals(expectedJson, actualJson)
        }

    @Test
    fun `Test JWT validation route when token has no sub claim`() =
        testApplication {
            application {
                this@application.install(ContentNegotiation) {
                    json()
                }
                this@application.routing {
                    setUpJWTValidation("/validate")
                }
            }
            val response =
                client.get("/validate") {
                    header(HttpHeaders.Authorization, "Bearer ${AuthenticationTestHelpers.generateTestJwtTokenNoSub()}")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test JWT validation route returns empty response without credentials`() =
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
