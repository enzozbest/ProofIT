import kcl.seg.rtt.auth.AuthenticatedSession
import kcl.seg.rtt.auth.CognitoUserInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class HelpersTest {

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
}