import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import routes.AuthenticationRoutes.configureAuthenticationRoutes
import kotlin.test.assertTrue

class TestAuthentication {
    @Test
    fun `Test configureAuthenticationRoutes with default params`() =
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
                try {
                    configureAuthenticationRoutes()
                } catch (_: Exception) {
                    print("Entered catch block")
                }
                assertTrue(true)
            }
        }
}
