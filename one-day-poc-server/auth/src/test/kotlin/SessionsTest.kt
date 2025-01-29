import kcl.seg.rtt.auth.AuthenticatedSession
import kcl.seg.rtt.auth.GuestSession
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SessionsTest {


    @Test
    fun `Test Guest Session Class`() {
        val session = GuestSession("userId", 1)
        assertEquals("userId", session.guestId)
        assertEquals("GuestSession", session.getSessionType())
        assertNull(session.getAccessToken())
        assertEquals(1, session.submissions)
        assertFalse { session.isAdmin() }
    }

    @Test
    fun `Test Authenticated Session Class`() {
        val session = AuthenticatedSession("userId", "token", false)
        assertEquals("userId", session.userId)
        assertEquals("AuthenticatedSession", session.getSessionType())
        assertEquals("token", session.getAccessToken())
        assertNull(session.getSubmissions())
        assertFalse { session.isAdmin() }
    }

}