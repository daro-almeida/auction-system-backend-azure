package scc.kube;

import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.AppLogic;
import scc.MediaId;
import scc.kube.config.RedisConfig;
import scc.kube.dao.UserDao;
import scc.kube.utils.ObjectIdModule;
import scc.utils.Hash;

public class Kube {
    public static Jedis createJedis(RedisConfig config) {
        var jedis = new Jedis(config.url, config.port);
        return jedis;
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
        return new JedisPool(poolConfig, config.url, config.port, 1000);
    }

    static String hashUserPassword(String password) {
        return Hash.of(password);
    }

    static String mediaIdToString(MediaId mediaId) {
        return mediaId.getId();
    }

    static MediaId stringToMediaId(String mediaId) {
        return new MediaId(mediaId);
    }

    static String userDisplayNameFromDao(UserDao userDao) {
        if (userDao.status == UserDao.Status.ACTIVE)
            return userDao.username;
        else
            return AppLogic.DELETED_USER_ID;
    }
}
