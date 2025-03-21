package authentication.redis

import redis.clients.jedis.Jedis

/**
 * Interface for providing Redis connections.
 * This interface is used to abstract the Redis connection creation,
 * increasing testability.
 */
interface RedisProvider {
    /**
     * Get a Redis connection.
     * @return A Jedis instance representing a Redis connection.
     */
    fun getRedisConnection(): Jedis
}
