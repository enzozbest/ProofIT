package kcl.seg.rtt.prototype

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kcl.seg.rtt.webcontainer.WebContainer
import org.junit.jupiter.api.Test
import kotlin.test.*

import io.ktor.server.application.install


class WebContainerTest {

    private val webContainer = WebContainer()

    @Test
    fun `test parsePrototype returns input unchanged`() {
        val testContent = "<html>test</html>"
        val result = webContainer::class.java.getDeclaredMethod("parsePrototype", String::class.java).apply {
            isAccessible = true
        }.invoke(webContainer, testContent) as String

        assertEquals(testContent, result)
    }

    @Test
    fun `test webcontainer route with valid id`() = testApplication {
        application {
            install(ContentNegotiation)
            routing {
                with(webContainer) {
                    webcontainerRoutes()
                }
            }
        }

        val response = client.get("/webcontainer/123")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("<html><body><h1>Hello from Prototype 123</h1></body></html>", response.bodyAsText())
        assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), response.contentType())
    }

    @Test
    fun `test webcontainer route with missing id`() = testApplication {
        application {
            routing {
                with(webContainer) {
                    webcontainerRoutes()
                }
            }
        }

        val response = client.get("/webcontainer/")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `test webcontainer route with blank id`() = testApplication {
        application {
            routing {
                with(webContainer) {
                    webcontainerRoutes()
                }
            }
        }

        val response = client.get("/webcontainer/%20") // URL encoded space
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Missing ID.", response.bodyAsText())
    }

    @Test
    fun `test getValidPrototypeIdOrRespond with valid id`() = testApplication {
        application {
            routing {
                get("/test/{id}") {
                    val id = webContainer.getValidPrototypeIdOrRespond(call)
                    call.respond(id ?: "invalid")
                }
            }
        }

        val response = client.get("/test/valid-id")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("valid-id", response.bodyAsText())
    }

    @Test
    fun `test getValidPrototypeIdOrRespond with missing id`() = testApplication {
        application {
            routing {
                get("/test") {
                    val id = webContainer.getValidPrototypeIdOrRespond(call)
                    call.respond(id ?: "invalid")
                }
            }
        }

        val response = client.get("/test")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Missing ID.", response.bodyAsText())
    }

    @Test
    fun `test getValidPrototypeIdOrRespond with blank id`() = testApplication {
        application {
            routing {
                get("/test/{id}") {
                    val id = webContainer.getValidPrototypeIdOrRespond(call)
                    call.respond(id ?: "invalid")
                }
            }
        }

        val response = client.get("/test/%20") // URL encoded space
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Missing ID.", response.bodyAsText())
    }


}