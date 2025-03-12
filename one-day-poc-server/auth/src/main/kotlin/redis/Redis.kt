package redis

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

internal object Redis {
    private const val REDIS_HOST = "localhost"
    private const val REDIS_PORT = 6379

    private val redisPool = JedisPool(JedisPoolConfig(), REDIS_HOST, REDIS_PORT)

    fun getRedisConnection(): Jedis = redisPool.resource
}
