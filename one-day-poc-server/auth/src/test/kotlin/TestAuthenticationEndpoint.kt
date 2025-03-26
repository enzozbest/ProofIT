import authentication.authentication.setAuthenticationEndpoint
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestAuthenticationEndpoint {
    @Test
    fun `Test authentication endpoint redirects to authenticate`() = testApplication {
        routing {
            setAuthenticationEndpoint("/auth")
        }
        
        val client = createClient {
            followRedirects = false
        }
        
        val response = client.get("/auth")
        
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("/authenticate", response.headers["Location"])
    }
}