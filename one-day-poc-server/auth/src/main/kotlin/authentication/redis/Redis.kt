package authentication.redis

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import utils.environment.EnvironmentLoader

/**
 * Object that provides Redis connections.
 */
internal object Redis {
    private val REDIS_HOST = EnvironmentLoader.get("REDIS_HOST")
    private const val REDIS_PORT = 6379
    private val redisPool = JedisPool(JedisPoolConfig(), REDIS_HOST, REDIS_PORT)

    fun getRedisConnection(): Jedis = redisPool.resource
}
