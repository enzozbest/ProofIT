import authentication.authentication.AuthenticatedSession
import authentication.authentication.CognitoUserInfo
import authentication.authentication.JWTValidationResponse
import authentication.authentication.buildUserInfoRequest
import authentication.authentication.generateUserInfo
import authentication.authentication.sendRequest
import authentication.authentication.validateJWT
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.test.*

class TestAuthenticationHelpers {
    @Test
    fun `Test AuthenticatedSession Class`() {
        val session = AuthenticatedSession("userId", "token", false)
        assertEquals("userId", session.userId)
        assertEquals("token", session.token)
        assertFalse(session.admin!!)
    }

    @Test
    fun `Test AuthenticatedSession Class with null admin`() {
        val session = AuthenticatedSession("userId", "token", null)
        assertEquals("userId", session.userId)
        assertEquals("token", session.token)
        assertNull(session.admin)
    }

    @Test
    fun `Test CognitoUserInfo Class`() {
        val cognitoUser = CognitoUserInfo("name", "email@example.org", "dd-mm-yyyy")
        assertEquals("name", cognitoUser.name)
        assertEquals("email@example.org", cognitoUser.email)
        assertEquals("dd-mm-yyyy", cognitoUser.dob)
    }

    @Test
    fun `Test JWTValidationResponse Class`() {
        val response = JWTValidationResponse("userId", false)
        assertEquals("userId", response.userId)
        assertFalse(response.admin!!)
    }

    @Test
    fun `Test serialization of AuthenticatedSession`() {
        val session = AuthenticatedSession(userId = "12345", token = "abcdef", admin = true)
        val json = Json.encodeToString<AuthenticatedSession>(session)
        val expectedJson = """{"userId":"12345","token":"abcdef","admin":true}"""
        assertEquals(expectedJson, json, "Serialization failed")
    }

    @Test
    fun `Test deserialization of AuthenticatedSession`() {
        val json = """{"userId":"12345","token":"abcdef","admin":true}"""
        val session = Json.decodeFromString<AuthenticatedSession>(json)
        val expectedSession = AuthenticatedSession(userId = "12345", token = "abcdef", admin = true)
        assertEquals(expectedSession, session, "Deserialization failed")
    }

    @Test
    fun `Test serialization of CognitoUserInfo`() {
        val userInfo = CognitoUserInfo(name = "name", email = "email@example.org", dob = "dd-mm-yyyy")
        val json = Json.encodeToString<CognitoUserInfo>(userInfo)
        val expectedJson = """{"name":"name","email":"email@example.org","dob":"dd-mm-yyyy"}"""
        assertEquals(expectedJson, json, "Serialization failed")
    }

    @Test
    fun `Test deserialization of CognitoUserInfo`() {
        val json = """{"name":"name","email":"email@example.org","dob":"dd-mm-yyyy"}"""
        val userInfo = Json.decodeFromString<CognitoUserInfo>(json)
        val expectedInfo = CognitoUserInfo(name = "name", email = "email@example.org", dob = "dd-mm-yyyy")
        assertEquals(expectedInfo, userInfo, "Deserialization failed")
    }

    @Test
    fun `Test serialization of JWTValidationResponse`() {
        val response = JWTValidationResponse("userId", false)
        val json = Json.encodeToString<JWTValidationResponse>(response)
        val expectedJson = """{"userId":"userId","admin":false}"""
        assertEquals(expectedJson, json, "Serialization failed")
    }

    @Test
    fun `Test deserialization of JWTValidationResponse`() {
        val json = """{"userId":"userId","admin":false}"""
        val response = Json.decodeFromString<JWTValidationResponse>(json)
        val expectedResponse = JWTValidationResponse("userId", false)
        assertEquals(expectedResponse, response, "Deserialization failed")
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test AuthenticatedSession deserialization missing a field`() {
        val json = """{"userId": "12345", "token": "abcdef"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<AuthenticatedSession>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test CognitoUserInfo deserialization missing a field`() {
        val json = """{"name": "name", "email": "email@example.org"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<CognitoUserInfo>(json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test JWTValidationResponse deserialization missing a field`() {
        val json = """{"userId": "userId"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<JWTValidationResponse>(json)
        }
    }

    @Test
    fun `Test generateUserInfo with valid user attributes`() {
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
        val response = createResponse(json)
        val userInfo = generateUserInfo(response)
        assertEquals("John Doe", userInfo.name)
        assertEquals("john.doe@example.com", userInfo.email)
        assertEquals("1990-01-01", userInfo.dob)
    }

    @Test
    fun `Test generateUserInfo when UserAttributes key is missing`() {
        val json =
            """
            {
                "OtherKey": []
            }
            """.trimIndent()
        val response = createResponse(json)
        val userInfo = generateUserInfo(response)
        assertEquals("", userInfo.name)
        assertEquals("", userInfo.email)
        assertEquals("", userInfo.dob)
    }

    @Test
    fun `Test generateUserInfo with empty UserAttributes array`() {
        val json =
            """
            {
                "UserAttributes": []
            }
            """.trimIndent()
        val response = createResponse(json)
        val userInfo = generateUserInfo(response)
        assertEquals("Unknown", userInfo.name)
        assertEquals("Unknown", userInfo.email)
        assertEquals("Unknown", userInfo.dob)
    }

    @Test
    fun `Test generateUserInfo with partial attributes`() {
        val json =
            """
            {
                "UserAttributes": [
                    {"Name": "name", "Value": "Alice"}
                ]
            }
            """.trimIndent()
        val response = createResponse(json)
        val userInfo = generateUserInfo(response)
        assertEquals("Alice", userInfo.name)
        assertEquals("Unknown", userInfo.email)
        assertEquals("Unknown", userInfo.dob)
    }

    @Test
    fun `Test generateUserInfo with no UserAttributes`() {
        val json =
            """
            {

            }
            """.trimIndent()
        val response = createResponse(json)
        val userInfo = generateUserInfo(response)
        assertEquals("", userInfo.name)
        assertEquals("", userInfo.email)
        assertEquals("", userInfo.dob)
    }

    @Test
    fun `Test generateUserInfo with invalid JSON body`() {
        val response = createResponse("")
        assertFailsWith<Exception> {
            generateUserInfo(response)
        }
    }

    @Test
    fun `Test generateUserInfo with non-array UserAttributes`() {
        val json =
            """
            {
                "UserAttributes": "not an array"
            }
            """.trimIndent()
        val response = createResponse(json)
        assertFailsWith<Exception> {
            generateUserInfo(response)
        }
    }

    @Test
    fun `Test generateUserInfo with non-JsonObject`() {
        val json =
            """
            [
                {"UserAttributes": []}
            ]
            """.trimIndent()
        val response = createResponse(json)
        assertFailsWith<Exception> {
            generateUserInfo(response)
        }
    }

    @Test
    fun `Test generateUserInfo with UserAttributes explicitly set to null`() {
        val json =
            """
            {
                "UserAttributes": null
            }
            """.trimIndent()
        val response = createResponse(json)
        assertFailsWith<Exception> {
            generateUserInfo(response)
        }
    }

    @Test
    fun `Test sendRequest() returns a response`() {
        val mockWebServer = MockWebServer()
        mockWebServer.start(port = 65530)
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val request =
            Request
                .Builder()
                .url(mockWebServer.url("/test"))
                .build()

        val response = request.sendRequest()

        assertTrue(response.isSuccessful)
        assertEquals(200, response.code)
        assertEquals("Success", response.body.string())
    }

    @Test
    fun `Test buildUserInfoRequest builds request with correct headers when amzTarget is true`() {
        val request =
            buildUserInfoRequest(
                token = "test-token",
                verifierUrl = "https://example.com",
                accessKey = "test-access-key",
                secretKey = "test-secret-key",
                region = "test-region",
                contentType = "application/json",
                amzTarget = true,
                amzApi = "AWSCognitoIdentityProviderService.GetUser",
                amzDate = "20230101T000000Z",
                dateStamp = "20230101",
            )
        assertEquals("https://example.com/", request.url.toString())
        assertNotNull(request.header("Authorization"))
        assertTrue(request.header("Authorization")!!.startsWith("AWS4-HMAC-SHA256"))
        assertEquals("application/json", request.header("Content-Type"))
        assertEquals("AWSCognitoIdentityProviderService.GetUser", request.header("X-Amz-Target"))
    }

    @Test
    fun `Test buildUserInfoRequest does not add X-Amz-Target header when amzTarget is false`() {
        val request =
            buildUserInfoRequest(
                token = "test-token",
                verifierUrl = "https://example.com",
                accessKey = "test-access-key",
                secretKey = "test-secret-key",
                region = "test-region",
                contentType = "application/json",
                amzTarget = false,
                amzApi = "AWSCognitoIdentityProviderService.GetUser",
                amzDate = "20230101T000000Z",
                dateStamp = "20230101",
            )
        assertEquals("https://example.com/", request.url.toString())
        assertNotNull(request.header("Authorization"))
        assertTrue(request.header("Authorization")!!.startsWith("AWS4-HMAC-SHA256"))
        assertEquals("application/json", request.header("Content-Type"))
        assertNull(request.header("X-Amz-Target")) // Header should not exist
    }

    @Test
    fun `Test buildUserInfoRequest handles empty token`() {
        val request =
            buildUserInfoRequest(
                token = "",
                verifierUrl = "https://example.com",
                accessKey = "test-access-key",
                secretKey = "test-secret-key",
                region = "test-region",
                contentType = "application/json",
                amzTarget = true,
                amzApi = "AWSCognitoIdentityProviderService.GetUser",
                amzDate = "20230101T000000Z",
                dateStamp = "20230101",
            )
        assertNotNull(request.header("Authorization"))
        assertTrue(request.header("Authorization")!!.startsWith("AWS4-HMAC-SHA256"))
    }

    @Test
    fun `Test buildUserInfoRequest handles different content types`() {
        val request =
            buildUserInfoRequest(
                token = "test-token",
                verifierUrl = "https://example.com",
                accessKey = "test-access-key",
                secretKey = "test-secret-key",
                region = "test-region",
                contentType = "text/plain",
                amzTarget = true,
                amzApi = "AWSCognitoIdentityProviderService.GetUser",
                amzDate = "20230101T000000Z",
                dateStamp = "20230101",
            )
        assertEquals("text/plain", request.header("Content-Type"))
        assertNotNull(request.header("Authorization"))
        assertTrue(request.header("Authorization")!!.startsWith("AWS4-HMAC-SHA256"))
    }

    @Test
    fun `Test JWT validation with invalid JWT`() {
        val response = validateJWT("invalid.jwt")
        assertNull(response)
    }

    @Test
    fun `Test JWT validation with expired JWT`() {
        val jwt =
            JWT
                .create()
                .withClaim("sub", "user123")
                .withExpiresAt(Date(0))
                .sign(Algorithm.none())
        val response = validateJWT(jwt)
        assertNull(response)
    }

    @Test
    fun `Test JWT validation with no userId JWT`() {
        val jwt = JWT.create().withExpiresAt(Date.from(Instant.now().plusSeconds(3600))).sign(Algorithm.none())
        val response = validateJWT(jwt)
        assertNull(response)
    }

    @Test
    fun `Test JWT validation with admin group`() {
        val jwt =
            JWT
                .create()
                .withClaim("sub", "user123")
                .withClaim("cognito:groups", listOf("admin_users"))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(Algorithm.none())
        val response = validateJWT(jwt)
        assertEquals(JWTValidationResponse("user123", true), response)
    }

    @Test
    fun `Test JWT validation without admin group`() {
        val jwt =
            JWT
                .create()
                .withClaim("sub", "user123")
                .withClaim("cognito:groups", listOf("regular_users"))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(Algorithm.none())
        val response = validateJWT(jwt)
        assertEquals(JWTValidationResponse("user123", false), response)
    }

    @Test
    fun `Test JWT validation with cognito_groups not a list of strings`() {
        val jwt =
            JWT
                .create()
                .withClaim("sub", "user123")
                .withClaim("cognito:groups", listOf(1, 2, 3, 4, 5))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(Algorithm.none())
        val response = validateJWT(jwt)
        assertEquals(JWTValidationResponse("user123", false), response)
    }

    @Test
    fun `Test JWT validation without cognito_groups`() {
        val jwt =
            JWT
                .create()
                .withClaim("sub", "user123")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(Algorithm.none())
        val response = validateJWT(jwt)
        assertEquals(JWTValidationResponse("user123", false), response)
    }

    private fun createResponse(body: String): Response =
        Response
            .Builder()
            .request(Request.Builder().url("http://localhost/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(
                body.toResponseBody("application/json".toMediaType()),
            ).build()
}
