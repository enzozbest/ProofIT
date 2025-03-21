package helpers

import authentication.redis.RedisProvider
import io.mockk.every
import io.mockk.mockk
import redis.clients.jedis.Jedis

/**
 * A mock implementation of RedisProvider for testing.
 * It uses mockk to create a mock Jedis instance that can be configured to return specific values.
 */
class MockRedisProvider : RedisProvider {
    // In-memory storage for the mock Redis
    private val storage = mutableMapOf<String, String>()

    // Mock Jedis instance
    private val mockJedis = mockk<Jedis>(relaxed = true)

    init {
        // Configure the mock Jedis instance to use the in-memory storage
        every { mockJedis.setex(any<String>(), any<Long>(), any<String>()) } answers {
            val key = arg<String>(0)
            val value = arg<String>(2)
            storage[key] = value
            "OK"
        }

        every { mockJedis.get(any<String>()) } answers {
            val key = arg<String>(0)
            storage[key]
        }

        every { mockJedis.del(any<String>()) } answers {
            val key = arg<String>(0)
            if (storage.containsKey(key)) {
                storage.remove(key)
                1L
            } else {
                0L
            }
        }

        // Support for the [] operator (used in checkCache)
        every { mockJedis.get(any<String>()) } answers {
            val key = arg<String>(0)
            storage[key]
        }
    }

    override fun getRedisConnection(): Jedis = mockJedis
}
