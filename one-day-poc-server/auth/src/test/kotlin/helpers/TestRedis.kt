package helpers

import authentication.authentication.JWTValidationResponse
import authentication.authentication.cacheSession
import authentication.authentication.checkCache
import authentication.redis.Redis
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import utils.environment.EnvironmentLoader

class TestRedis {
    private val redisContainer =
        GenericContainer<Nothing>("redis:7").apply { withExposedPorts(6379) }

    @BeforeEach
    fun setUp() {
        mockkObject(EnvironmentLoader)
        every { EnvironmentLoader.get("REDIS_HOST") } returns redisContainer.host
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(EnvironmentLoader)
    }

    @Test
    fun `test Redis connection works`() {
        val jedis = Redis.getRedisConnection()
        val key = "test_key"
        val value = "test_value"
        jedis.setex(key, 60, value)

        val retrieved = jedis.get(key)
        assertNotNull(jedis)
        assertEquals(value, retrieved)

        jedis.close()
    }

    @Test
    fun `checkCache should return null when Json decoding fails`() {
        val redis = Redis.getRedisConnection()
        val token = "test_token"
        val invalidJson = "{invalid json data}"
        redis.setex("auth:$token", 60, invalidJson)
        val result = checkCache(token)
        assertNull(result, "Should return null when JSON decoding fails")
        redis.del("auth:$token")
    }

    @Test
    fun `checkCache should return null when token is not found`() {
        val redis = Redis.getRedisConnection()
        val token = "test_token"
        val validJson = "{\"userId\":\"123\", \"admin\":true}"
        redis.setex(token, 60, validJson)

        val result = checkCache(token)
        assertNull(result, "Should not have found token without auth: prefix")
        redis.del(token)
    }

    @Test
    fun `checkCache should return data everything is valid`() {
        val redis = Redis.getRedisConnection()
        val token = "test_token"
        val validJson = "{\"userId\":\"123\", \"admin\":true}"
        redis.setex("auth:$token", 60, validJson)

        val result = checkCache(token)
        assertNotNull(result, "Should have decoded the JSON properly")
        assertEquals("123", result!!.userId, "Should have the correct userId")
        assertEquals(true, result.admin, "Should have the correct admin status")
        redis.del("auth:$token")
    }

    @Test
    fun `cacheSession correctly stores session details with default expiration`() {
        val redis = Redis.getRedisConnection()
        val mockData = JWTValidationResponse("123", true)
        val token = "test_token"

        cacheSession(token, mockData)

        val result = redis.get("auth:$token")
        assertNotNull(result)
        assertEquals("{\"userId\":\"123\",\"admin\":true}", result)
        redis.del("auth:$token")
    }
}
