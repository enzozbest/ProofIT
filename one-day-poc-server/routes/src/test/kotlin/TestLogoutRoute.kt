import authentication.authentication.AuthenticationRoutes.LOG_OUT_ROUTE
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import routes.AuthenticationRoutes.configureAuthenticationRoutes
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestLogoutRoute {
    @Test
    fun `Test Log Out Route exists`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("Cognito") {
                        validate { UserIdPrincipal("test") }
                    }
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureAuthenticationRoutes()
            }
            val response = client.post(LOG_OUT_ROUTE)
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test Logout route without session cookie`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("Cognito") {
                        validate { UserIdPrincipal("test") }
                    }
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureAuthenticationRoutes()
            }
            val response = client.post(LOG_OUT_ROUTE)
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test Logout route with invalid session cookie`() =
        testApplication {
            application {
                install(Authentication) {
                    basic("Cognito") {
                        validate { UserIdPrincipal("test") }
                    }
                    basic("jwt-verifier") {
                        validate { UserIdPrincipal("test") }
                    }
                }
                install(Sessions) {}
                configureAuthenticationRoutes()
            }
            val response =
                client.post(LOG_OUT_ROUTE) {
                    cookie("AuthenticatedSession", "invalid")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }
}
