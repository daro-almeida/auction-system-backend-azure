package scc.cache.redis;

import scc.azure.config.RedisConfig;
import scc.cache.UserCache;

public class RedisUserCache implements UserCache {

    private final RedisCache redisCache;

    public RedisUserCache(RedisConfig config) {
        this.redisCache = RedisCache.getInstance(config);
    }
    @Override
    public void deleteUser(String userId) {
        redisCache.del(USER_PREFIX + userId);
    }
}
