package scc.azure;

import java.util.Optional;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.azure.config.RedisConfig;
import scc.azure.dao.QuestionDAO;
import scc.services.data.QuestionItem;
import scc.services.data.ReplyItem;
import scc.utils.Hash;

public interface AzureUtils {
    public static String hashUserPassword(String password) {
        return Hash.of(password);
    }

    public static JedisPool createJedisPool(RedisConfig config) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return new JedisPool(poolConfig, config.url, 6380, 1000, config.key, true);
    }
}
