package scc.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.azure.config.RedisConfig;

import java.util.Optional;

public class RedisCache implements Cache {

    private final JedisPool pool;

    public RedisCache(RedisConfig config) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        pool = new JedisPool(poolConfig, config.url, 6380, 1000, config.key, true);

    }

    @Override
    public String get(String key) {
        var jedis = getClient();
        return jedis.get(key);
    }

    @Override
    public String set(String key, String value) {
        var jedis = getClient();
        return jedis.set(key, value);
    }

    @Override
    public Long expire(String key, int seconds) {
        var jedis = getClient();
        return jedis.expire(key, seconds);
    }

    @Override
    public Long del(String... keys) {
        var jedis = getClient();
        return jedis.del(keys);
    }

    @Override
    public Long deleteAuction(String auctionId) {
        return del(AUCTION_PREFIX + auctionId);
    }

    @Override
    public Long deleteQuestion(String questionId) {
        return del(QUESTION_PREFIX + questionId);
    }

    @Override
    public Long deleteUser(String userId) {
        return del(USER_PREFIX + userId);
    }

    @Override
    public String setBytes(String key, byte[] value){
        var jedis = getClient();
        return jedis.set(key.getBytes(), value);
    }

    @Override
    public Optional<byte[]> getBytes(String key){
        var jedis = getClient();
        return Optional.of(jedis.get(key.getBytes()));
    }

    @Override
    public Long deleteMedia(String mediaId){
        if(mediaId.startsWith(AUCTION_PREFIX))
            return del(AUCTION_MEDIA_PREFIX + mediaId);
        else return del(USER_MEDIA_PREFIX + mediaId);
    }

    private Jedis getClient() {
        return pool.getResource();
    }
}
