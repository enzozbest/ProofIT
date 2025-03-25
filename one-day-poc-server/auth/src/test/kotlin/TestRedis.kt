import authentication.redis.Redis
import helpers.AuthenticationTestHelpers.resetMockRedis
import helpers.AuthenticationTestHelpers.setUpMockRedis
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import kotlin.test.assertNotNull

class TestRedis {
    private lateinit var mockJedis: Jedis

    @BeforeEach
    fun setUp() {
        mockJedis = setUpMockRedis()
    }

    @AfterEach
    fun tearDown() {
        resetMockRedis(mockJedis)
    }

    @Test
    fun `Test getRedisConnection returns a valid connection`() {
        val connection = Redis.getRedisConnection()
        assertNotNull(connection, "Redis connection should not be null")
        connection.close() // Close the connection after use
    }
}