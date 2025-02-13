import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
<<<<<<< HEAD
import kcl.seg.rtt.auth.AuthenticatedSession
import kcl.seg.rtt.auth.setUpUserInfoRoute
=======
import kcl.seg.rtt.auth.authentication.AuthenticatedSession
import kcl.seg.rtt.auth.authentication.setUpUserInfoRoute
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestUserInfoRoute {
    private lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start(port = 16000)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
<<<<<<< HEAD
    fun `Test returns 401 when authentication cookie is missing`(): Unit = testApplication {
        routing {
            setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
        }
        client.get("/userinfo").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
            assertEquals("Missing authentication cookie", bodyAsText())
        }
    }

    @Test
    fun `Test returns 401 when authentication token format is invalid`() = testApplication {
        routing {
            setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
        }
        val client = createClient {
            followRedirects = false
        }
        client.get("/userinfo") {
            header(HttpHeaders.Cookie, "AuthenticatedSession=invalid-json")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
            assertEquals("Invalid token format", bodyAsText())
        }
    }

    @Test
    fun `Test returns 401 when token is invalid`() = testApplication {
        routing {
            setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        val client = createClient {
            followRedirects = false
        }
        client.get("/userinfo") {
            header(
                HttpHeaders.Cookie,
                "AuthenticatedSession=${
                    Json.encodeToString<AuthenticatedSession>(
                        AuthenticatedSession(
                            "test-user",
                            "invalid token",
                            false
                        )
                    )
                }"
            )
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
            assertEquals("Invalid token", bodyAsText())
        }
    }
    
    @Test
    fun `Test returns 500 on unexpected error`() = testApplication {
        routing {
            setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val client = createClient {
            followRedirects = false
        }
        val cookieValue = Json.encodeToString(
            AuthenticatedSession("test-user", "valid token", false)
        )
        client.get("/userinfo") {
            header(HttpHeaders.Cookie, "AuthenticatedSession=$cookieValue")
        }.apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
        }
    }

    @Test
    fun `Test returns 200 when request is successful`() = testApplication {
        routing {
            setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
        }
        val json = """
        {
            "UserAttributes": [
                {"Name": "name", "Value": "John Doe"},
                {"Name": "email", "Value": "john.doe@example.com"},
                {"Name": "birthdate", "Value": "1990-01-01"}
            ]
        }
    """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))
        val client = createClient {
            followRedirects = false
        }
        val cookieValue = Json.encodeToString(
            AuthenticatedSession("test-user", "valid token", false)
        )
        client.get("/userinfo") {
            header(HttpHeaders.Cookie, "AuthenticatedSession=$cookieValue")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertNotNull(bodyAsText())
        }
    }
}
=======
    fun `Test returns 401 when authentication cookie is missing`(): Unit =
        testApplication {
            routing {
                setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
            }
            client.get("/userinfo").apply {
                assertEquals(HttpStatusCode.Unauthorized, status)
                assertEquals("Missing authentication cookie", bodyAsText())
            }
        }

    @Test
    fun `Test returns 401 when authentication token format is invalid`() =
        testApplication {
            routing {
                setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
            }
            val client =
                createClient {
                    followRedirects = false
                }
            client
                .get("/userinfo") {
                    header(HttpHeaders.Cookie, "AuthenticatedSession=invalid-json")
                }.apply {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                    assertEquals("Invalid token format", bodyAsText())
                }
        }

    @Test
    fun `Test returns 401 when token is invalid`() =
        testApplication {
            routing {
                setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
            }
            mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
            val client =
                createClient {
                    followRedirects = false
                }
            client
                .get("/userinfo") {
                    header(
                        HttpHeaders.Cookie,
                        "AuthenticatedSession=${
                            Json.encodeToString<AuthenticatedSession>(
                                AuthenticatedSession(
                                    "test-user",
                                    "invalid token",
                                    false,
                                ),
                            )
                        }",
                    )
                }.apply {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                    assertEquals("Invalid token", bodyAsText())
                }
        }

    @Test
    fun `Test returns 500 on unexpected error`() =
        testApplication {
            routing {
                setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
            }
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            val client =
                createClient {
                    followRedirects = false
                }
            val cookieValue =
                Json.encodeToString(
                    AuthenticatedSession("test-user", "valid token", false),
                )
            client
                .get("/userinfo") {
                    header(HttpHeaders.Cookie, "AuthenticatedSession=$cookieValue")
                }.apply {
                    assertEquals(HttpStatusCode.InternalServerError, status)
                }
        }

    @Test
    fun `Test returns 200 when request is successful`() =
        testApplication {
            routing {
                setUpUserInfoRoute("/userinfo", mockWebServer.url("/").toString())
            }
            val json =
                """
                {
                    "UserAttributes": [
                        {"Name": "name", "Value": "John Doe"},
                        {"Name": "email", "Value": "john.doe@example.com"},
                        {"Name": "birthdate", "Value": "1990-01-01"}
                    ]
                }
                """.trimIndent()
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))
            val client =
                createClient {
                    followRedirects = false
                }
            val cookieValue =
                Json.encodeToString(
                    AuthenticatedSession("test-user", "valid token", false),
                )
            client
                .get("/userinfo") {
                    header(HttpHeaders.Cookie, "AuthenticatedSession=$cookieValue")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertNotNull(bodyAsText())
                }
        }
}
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
