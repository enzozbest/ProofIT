<<<<<<< HEAD
=======
import helpers.AuthenticationTestHelpers
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
<<<<<<< HEAD
import kcl.seg.rtt.auth.AuthenticatedSession
import kcl.seg.rtt.auth.setUpJWTValidation
import kotlinx.serialization.SerializationException
=======
import kcl.seg.rtt.auth.authentication.AuthenticatedSession
import kcl.seg.rtt.auth.authentication.setUpJWTValidation
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
<<<<<<< HEAD
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
=======

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
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
