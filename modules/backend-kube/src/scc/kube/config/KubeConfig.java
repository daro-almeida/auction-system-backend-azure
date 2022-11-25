package scc.kube.config;

public class KubeConfig {
    private final KubeMediaConfig mediaConfig;
    private final MongoConfig mongoConfig;
    private final RedisConfig redisConfig;
    private final boolean enableCaching;

    public KubeConfig(
            KubeMediaConfig mediaConfig,
            MongoConfig mongoConfig,
            RedisConfig redisConfig,
            boolean enableCaching) {
        this.mediaConfig = mediaConfig;
        this.mongoConfig = mongoConfig;
        this.redisConfig = redisConfig;
        this.enableCaching = enableCaching;
    }

    public KubeMediaConfig getMediaConfig() {
        return mediaConfig;
    }

    public MongoConfig getMongoConfig() {
        return mongoConfig;
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public boolean isCachingEnabled() {
        return enableCaching;
    }

    @Override
    public String toString() {
        return "KubeConfig [mediaConfig=" + mediaConfig + ", mongoConfig=" + mongoConfig + ", redisConfig="
                + redisConfig + ", enableCaching=" + enableCaching + "]";
    }

}
