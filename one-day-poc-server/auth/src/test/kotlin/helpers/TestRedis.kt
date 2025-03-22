package helpers

import authentication.redis.Redis
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis

class TestRedis {
    private lateinit var connectionMock: Jedis

    @BeforeEach
    fun setUp() {
        connectionMock = AuthenticationTestHelpers.setUpMockRedis()
    }

    @AfterEach
    fun tearDown() {
        AuthenticationTestHelpers.resetMockRedis()
    }

    @Test
    fun `Test Redis returns correct connection`() {
        val connection = Redis.getRedisConnection()
        assert(connection == connectionMock)
    }
}
