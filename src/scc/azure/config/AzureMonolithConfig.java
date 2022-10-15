package scc.azure.config;

import java.util.Optional;

public class AzureMonolithConfig {
    private final BlobStoreConfig blobStoreConfig;
    private final CosmosDbConfig cosmosDbConfig;
    private Optional<RedisConfig> redisConfig;

    public AzureMonolithConfig(BlobStoreConfig blobStoreConfig, CosmosDbConfig cosmosDbConfig) {
        this.blobStoreConfig = blobStoreConfig;
        this.cosmosDbConfig = cosmosDbConfig;
        this.redisConfig = Optional.empty();
    }

    public AzureMonolithConfig enableCaching(String redisKey, String redisUrl) {
        this.redisConfig = Optional.of(new RedisConfig(redisKey, redisUrl));
        return this;
    }

    public BlobStoreConfig getBlobStoreConfig() {
        return blobStoreConfig;
    }

    public CosmosDbConfig getCosmosDbConfig() {
        return cosmosDbConfig;
    }

    public Optional<RedisConfig> getRedisConfig() {
        return redisConfig;
    }

    public boolean isCachingEnabled() {
        return redisConfig.isPresent();
    }
}