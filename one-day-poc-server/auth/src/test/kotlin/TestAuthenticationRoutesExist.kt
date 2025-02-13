import helpers.AuthenticationTestHelpers.setupExternalServices
import helpers.AuthenticationTestHelpers.urlProvider
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
<<<<<<< HEAD
import kcl.seg.rtt.auth.*
=======
import kcl.seg.rtt.auth.authentication.*
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.AUTHENTICATION_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.CALL_BACK_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.JWT_VALIDATION_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.LOG_OUT_ROUTE
import kcl.seg.rtt.auth.authentication.AuthenticationRoutes.USER_INFO_ROUTE
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestAuthenticationRoutesExist {
<<<<<<< HEAD

    @Test
    fun `Test Authentication Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(AUTHENTICATION_ROUTE)
        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider["authorizeUrl"]!!.jsonPrimitive.content))
    }

    @Test
    fun `Test Callback Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(CALL_BACK_ROUTE)
        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers["Location"]!!.startsWith(urlProvider["authorizeUrl"]!!.jsonPrimitive.content))
    }

    @Test
    fun `Test Authorize Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        setupExternalServices()
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(urlProvider["authorizeUrl"]!!.jsonPrimitive.content)
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `Test JWT Validation Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val myClient = createClient {
            followRedirects = false
        }
        val response = myClient.get(JWT_VALIDATION_ROUTE)
        assertNotNull(response)
    }

    @Test
    fun `Test UserInfo Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val response = client.get(USER_INFO_ROUTE)
        assertNotNull(response)
    }

    @Test
    fun `Test Log Out Route exists`() = testApplication {
        application {
            authModule(
                configFilePath = "src/test/resources/cognito-test.json",
                authName = "testAuth"
            )
        }
        val response = client.get(LOG_OUT_ROUTE)
        assertNotNull(response)
    }
}



=======
    @Test
    fun `Test Authentication Route exists`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "src/test/resources/cognito-test.json",
                    authName = "testAuth",
                )
            }
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get(AUTHENTICATION_ROUTE)
            assertEquals(HttpStatusCode.Found, response.status)
            assertTrue(response.headers["Location"]!!.startsWith(urlProvider["authorizeUrl"]!!.jsonPrimitive.content))
        }

    @Test
    fun `Test Callback Route exists`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "src/test/resources/cognito-test.json",
                    authName = "testAuth",
                )
            }
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get(CALL_BACK_ROUTE)
            assertEquals(HttpStatusCode.Found, response.status)
            assertTrue(response.headers["Location"]!!.startsWith(urlProvider["authorizeUrl"]!!.jsonPrimitive.content))
        }

    @Test
    fun `Test Authorize Route exists`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "src/test/resources/cognito-test.json",
                    authName = "testAuth",
                )
            }
            setupExternalServices()
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get(urlProvider["authorizeUrl"]!!.jsonPrimitive.content)
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test JWT Validation Route exists`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "src/test/resources/cognito-test.json",
                    authName = "testAuth",
                )
            }
            val myClient =
                createClient {
                    followRedirects = false
                }
            val response = myClient.get(JWT_VALIDATION_ROUTE)
            assertNotNull(response)
        }

    @Test
    fun `Test UserInfo Route exists`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "src/test/resources/cognito-test.json",
                    authName = "testAuth",
                )
            }
            val response = client.get(USER_INFO_ROUTE)
            assertNotNull(response)
        }

    @Test
    fun `Test Log Out Route exists`() =
        testApplication {
            application {
                authModule(
                    configFilePath = "src/test/resources/cognito-test.json",
                    authName = "testAuth",
                )
            }
            val response = client.get(LOG_OUT_ROUTE)
            assertNotNull(response)
        }
}
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
