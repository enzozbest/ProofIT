import chat.GET
import chat.JSON
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import routes.ChatRoutes.configureChatRoutes
import kotlin.test.assertNotNull

class TestChatRoutes {
    @Test
    fun `Test Chat History Route exists`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureChatRoutes()
            }
            val response = client.get(GET)
            assertNotNull(response)
        }

    @Test
    fun `Test Chat Conversation Route exists`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureChatRoutes()
            }
            val response = client.get("$GET/test-conversation")
            assertNotNull(response)
        }

    @Test
    fun `Test Chat Prototype Route exists`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureChatRoutes()
            }
            val response = client.get("$GET/test-conversation/test-message")
            assertNotNull(response)
        }

    @Test
    fun `Test JSON Route exists`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureChatRoutes()
            }
            val response =
                client.post(JSON) {
                    setBody("{}")
                    header("Content-Type", "application/json")
                }
            assertNotNull(response)
        }

    @Test
    fun `Test JSON Rename Route exists`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureChatRoutes()
            }
            val response =
                client.post("$JSON/test-conversation/rename") {
                    setBody("{\"name\":\"New Name\"}")
                    header("Content-Type", "application/json")
                }
            assertNotNull(response)
        }
}
