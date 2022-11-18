package scc.azure.config;

public class AzureMonolithConfig {
    private final BlobStoreConfig blobStoreConfig;
    private final CosmosDbConfig cosmosDbConfig;
    private final RedisConfig redisConfig;
    private final MessageBusConfig messageBusConfig;
    private boolean isCachingEnabled;

    public AzureMonolithConfig(
            BlobStoreConfig blobStoreConfig,
            CosmosDbConfig cosmosDbConfig,
            RedisConfig redisConfig,
            MessageBusConfig messageBusConfig) {
        this.blobStoreConfig = blobStoreConfig;
        this.cosmosDbConfig = cosmosDbConfig;
        this.redisConfig = redisConfig;
        this.messageBusConfig = messageBusConfig;
        this.isCachingEnabled = false;
    }

    public AzureMonolithConfig enableCaching() {
        this.isCachingEnabled = true;
        return this;
    }

    public BlobStoreConfig getBlobStoreConfig() {
        return blobStoreConfig;
    }

    public CosmosDbConfig getCosmosDbConfig() {
        return cosmosDbConfig;
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public MessageBusConfig getMessageBusConfig() {
        return messageBusConfig;
    }

    public boolean isCachingEnabled() {
        return this.isCachingEnabled;
    }
}