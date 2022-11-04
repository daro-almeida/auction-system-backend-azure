package scc.cache.redis;

import scc.azure.config.RedisConfig;
import scc.cache.QuestionCache;

public class RedisQuestionCache implements QuestionCache {

    private final RedisCache redisCache;

    public RedisQuestionCache(RedisConfig config) {
        this.redisCache = RedisCache.getInstance(config);
    }

}
