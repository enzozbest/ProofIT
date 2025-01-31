import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kcl.seg.rtt.utils.JSON.PoCJSON.readJsonFile
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object AuthenticationTestHelpers {

    val jsonConfig: JsonObject =
        readJsonFile("src/test/resources/cognito-test.json")

    val urlProvider: JsonObject = jsonConfig["providerLookup"]!!.jsonObject

    fun TestApplicationBuilder.setupExternalServices() {
        externalServices {
            hosts("http://example.com:2000") {
                routing {
                    install(ContentNegotiation) {
                        json()
                    }
                    get("/authorize") {
                        val mockPrincipalJson = """
                            accessToken = "mockAccessToken",
                            tokenType = "Bearer",
                            expiresIn = 3600,
                            refreshToken = "mockRefreshToken",
                        """.trimIndent()
                        call.respond(mockPrincipalJson)
                    }
                }
            }
        }
    }

    /**
     * Set up Basic authentication for testing purposes only
     */
    fun AuthenticationConfig.configureBasicAuthentication() {
        basic("testAuth") {
            validate { credentials ->
                if (credentials.name == "test" && credentials.password == "password") {
                    UserIdPrincipal(credentials.name)
                } else null
            }
        }
    }
}

