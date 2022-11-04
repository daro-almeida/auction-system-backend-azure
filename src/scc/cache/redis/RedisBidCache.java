package scc.cache.redis;

import scc.azure.config.RedisConfig;
import scc.cache.BidCache;

public class RedisBidCache implements BidCache {
    private final RedisCache redisCache;

    public RedisBidCache(RedisConfig config) {
        this.redisCache = RedisCache.getInstance(config);
    }
}
