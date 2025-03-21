package authentication.redis

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import utils.environment.EnvironmentLoader

/**
 * Default implementation of RedisProvider that uses a JedisPool to provide Redis connections.
 */
internal class DefaultRedisProvider : RedisProvider {
    private val redisPool = JedisPool(JedisPoolConfig(), REDIS_HOST, REDIS_PORT)

    override fun getRedisConnection(): Jedis = redisPool.resource

    companion object {
        private val REDIS_HOST = EnvironmentLoader.get("REDIS_HOST")
        private const val REDIS_PORT = 6379
    }
}

/**
 * Object that provides Redis connections.
 * In production, it uses a DefaultRedisProvider.
 * In tests, it can be configured to use a mock provider.
 */
internal object Redis : RedisProvider {
    private var provider: RedisProvider = DefaultRedisProvider()

    /**
     * Set a custom Redis provider.
     * This is useful for testing, where a mock provider can be used.
     * @param redisProvider The Redis provider to use.
     */
    fun setProvider(redisProvider: RedisProvider) {
        provider = redisProvider
    }

    /**
     * Reset the Redis provider to the default.
     * This is useful for cleaning up after tests.
     */
    fun resetProvider() {
        provider = DefaultRedisProvider()
    }

    override fun getRedisConnection(): Jedis = provider.getRedisConnection()
}
