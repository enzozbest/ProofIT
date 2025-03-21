package chat.routes

import chat.BaseAuthenticationServer
import chat.GET
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class GetHistoryTest : BaseAuthenticationServer() {
    @Test
    fun `Test unauthorized access`() =
        testApplication {
            setupTestApplication()
            val response = client.get(GET)
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test authorized access`() =
        testApplication {
            setupTestApplication()
            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    accept(ContentType.Application.Json)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("conversations"))
        }

    @Test
    fun `Test invalid token`() =
        testApplication {
            setupTestApplication()
            val response =
                client.get(GET) {
                    header(HttpHeaders.Authorization, "Bearer invalid-token")
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `Test respondText function directly`() =
        testApplication {
            application {
                routing {
                    get(GET) {
                        call.respondText("Hello, world!")
                    }
                }
            }

            val response = client.get(GET)
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello, world!", response.bodyAsText())
        }

    @Test
    fun `Test chatRoutes function directly`() =
        testApplication {
            application {
                routing {
                    chatRoutes()
                }
            }

            val response = client.get(GET)
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
        }

    @Test
    fun `Test chatRoutes extension function on Route object`() =
        testApplication {
            application {
                routing {
                    val route = this
                    route.chatRoutes()
                }
            }

            val response = client.get(GET)
            assertEquals(HttpStatusCode.NotAcceptable, response.status)
        }
}
