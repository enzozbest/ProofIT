import authentication.authentication.AuthenticationRoutes.AUTHENTICATION_ROUTE
import authentication.authentication.AuthenticationRoutes.CALL_BACK_ROUTE
import authentication.authentication.AuthenticationRoutes.JWT_VALIDATION_ROUTE
import authentication.authentication.AuthenticationRoutes.LOG_OUT_ROUTE
import authentication.authentication.AuthenticationRoutes.USER_INFO_ROUTE
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import routes.AuthenticationRoutes.configureAuthenticationRoutes
import kotlin.test.assertNotNull

class TestAuthenticationRoutes {
    @Test
    fun `Test Authentication Route exists`() =
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
            val response = client.get(AUTHENTICATION_ROUTE)
            assertNotNull(response)
        }

    @Test
    fun `Test Callback Route exists`() =
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
            val response = client.get(CALL_BACK_ROUTE)
            assertNotNull(response)
        }

    @Test
    fun `Test JWT Validation Route exists`() =
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
            val response = client.get(JWT_VALIDATION_ROUTE)
            assertNotNull(response)
        }

    @Test
    fun `Test UserInfo Route exists`() =
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
            val response = client.get(USER_INFO_ROUTE)
            assertNotNull(response)
        }

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
            val response = client.get(LOG_OUT_ROUTE)
            assertNotNull(response)
        }
}
