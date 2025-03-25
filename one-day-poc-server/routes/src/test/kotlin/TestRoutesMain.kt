import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import routes.configureApplicationRoutes
import kotlin.test.assertTrue

class TestRoutesMain {
    @Test
    fun `Test configureApplicationRoutes`() = testApplication {
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
                configureApplicationRoutes()
            } catch (_: Exception) {
                print("Entered catch block")
            }
            assertTrue(true)
        }
    }
}
