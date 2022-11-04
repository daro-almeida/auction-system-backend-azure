package scc.cache.redis;

import scc.azure.config.RedisConfig;
import scc.cache.MediaCache;

public class RedisMediaCache implements MediaCache {

    private final RedisCache redisCache;

    public RedisMediaCache(RedisConfig config) {
        this.redisCache = RedisCache.getInstance(config);
    }
}
