package scc.azure.config;

public class AzureConfig {
    private final BlobStoreConfig blobStoreConfig;
    private final CosmosDbConfig cosmosDbConfig;
    private final RedisConfig redisConfig;
    private final MessageBusConfig messageBusConfig;
    private final CognitiveSearchConfig cognitiveSearchConfig;
    private boolean isCachingEnabled;

    public AzureConfig(
            BlobStoreConfig blobStoreConfig,
            CosmosDbConfig cosmosDbConfig,
            RedisConfig redisConfig,
            MessageBusConfig messageBusConfig,
            CognitiveSearchConfig cognitiveSearchConfig) {
        this.blobStoreConfig = blobStoreConfig;
        this.cosmosDbConfig = cosmosDbConfig;
        this.redisConfig = redisConfig;
        this.messageBusConfig = messageBusConfig;
        this.cognitiveSearchConfig = cognitiveSearchConfig;
        this.isCachingEnabled = false;
    }

    public AzureConfig enableCaching() {
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

    public CognitiveSearchConfig getCognitiveSearchConfig() {
        return cognitiveSearchConfig;
    }

    public boolean isCachingEnabled() {
        return this.isCachingEnabled;
    }

    @Override
    public String toString() {
        return "AzureMonolithConfig [blobStoreConfig=" + blobStoreConfig + ", cosmosDbConfig=" + cosmosDbConfig
                + ", redisConfig=" + redisConfig + ", messageBusConfig=" + messageBusConfig + ", cognitiveSearchConfig="
                + cognitiveSearchConfig + ", isCachingEnabled=" + isCachingEnabled + "]";
    }

}