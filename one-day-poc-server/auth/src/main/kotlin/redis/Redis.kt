package redis

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object Redis {
    private const val REDIS_HOST = "localhost"
    private const val REDIS_PORT = 6379

    private val redisPool = JedisPool(JedisPoolConfig(), REDIS_HOST, REDIS_PORT)

    fun getRedisConnection() = redisPool.resource
}
