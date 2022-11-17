package scc.utils;

import scc.azure.config.AzureMonolithConfig;
import scc.azure.config.BlobStoreConfig;
import scc.azure.config.CosmosDbConfig;
import scc.azure.config.RedisConfig;

public class SccEnv {
    public static final String BACKEND_KIND = "BACKEND_KIND";
    public static final String BACKEND_KIND_MEM = "memory";
    public static final String BACKEND_KIND_AZURE = "azure";

    public static final String AZURE_BLOB_STORAGE_CONNECTION_STRING = "AZURE_BLOB_STORAGE_CONNECTION_STRING";
    public static final String AZURE_BLOB_STORAGE_AUCTION_MEDIA_CONTAINER_NAME = "AZURE_BLOB_STORAGE_AUCTION_MEDIA_CONTAINER_NAME";
    public static final String AZURE_BLOB_STORAGE_USER_MEDIA_CONTAINER_NAME = "AZURE_BLOB_STORAGE_USER_MEDIA_CONTAINER_NAME";

    public static final String AZURE_COSMOS_DB_KEY = "AZURE_COSMOS_DB_KEY";
    public static final String AZURE_COSMOS_DB_URL = "AZURE_COSMOS_DB_URL";
    public static final String AZURE_COSMOS_DB_DATABASE = "AZURE_COSMOS_DB_DATABASE";
    public static final String AZURE_COSMOS_DB_USER_CONTAINER_NAME = "AZURE_COSMOS_DB_USER_CONTAINER_NAME";
    public static final String AZURE_COSMOS_DB_AUCTION_CONTAINER_NAME = "AZURE_COSMOS_DB_AUCTION_CONTAINER_NAME";
    public static final String AZURE_COSMOS_DB_BID_CONTAINER_NAME = "AZURE_COSMOS_DB_BID_CONTAINER_NAME";
    public static final String AZURE_COSMOS_DB_QUESTION_CONTAINER_NAME = "AZURE_COSMOS_DB_QUESTION_CONTAINER_NAME";

    public static final String AZURE_ENABLE_CACHING = "AZURE_ENABLE_CACHING";
    public static final String AZURE_ENABLE_CACHING_YES = "1";
    public static final String AZURE_ENABLE_CACHING_NO = "0";

    public static final String AZURE_REDIS_KEY = "AZURE_REDIS_KEY";
    public static final String AZURE_REDIS_URL = "AZURE_REDIS_URL";

    public static final String getBackendKind() {
        return getEnvVar(BACKEND_KIND, BACKEND_KIND_MEM);
    }

    public static final AzureMonolithConfig getAzureMonolithConfig() {
        var config = new AzureMonolithConfig(getAzureBlobStoreConfig(), getAzureCosmosDbConfig(),
                getAzureRedisConfig());
        var enableCaching = getAzureEnableCaching();
        switch (enableCaching) {
            case AZURE_ENABLE_CACHING_YES:
                config.enableCaching();
                break;
            case AZURE_ENABLE_CACHING_NO:
                break;
            default:
                throw new RuntimeException(
                        "Invalid value for environment variable " + AZURE_ENABLE_CACHING);
        }
        return config;
    }

    public static final String getAzureEnableCaching() {
        return getEnvVar(AZURE_ENABLE_CACHING, AZURE_ENABLE_CACHING_NO);
    }

    public static final BlobStoreConfig getAzureBlobStoreConfig() {
        var azureBlobStorageConnectionString = getEnvVar(AZURE_BLOB_STORAGE_CONNECTION_STRING);
        var azureBlobStorageAuctionMediaContainerName = getEnvVar(
                AZURE_BLOB_STORAGE_AUCTION_MEDIA_CONTAINER_NAME);
        var azureBlobStorageUserMediaContainerName = getEnvVar(
                AZURE_BLOB_STORAGE_USER_MEDIA_CONTAINER_NAME);

        return new BlobStoreConfig(
                azureBlobStorageConnectionString,
                azureBlobStorageAuctionMediaContainerName,
                azureBlobStorageUserMediaContainerName);
    }

    public static final CosmosDbConfig getAzureCosmosDbConfig() {
        var azureCosmosDbKey = getEnvVar(AZURE_COSMOS_DB_KEY);
        var azureCosmosDbUrl = getEnvVar(AZURE_COSMOS_DB_URL);
        var azureCosmosDbDatabase = getEnvVar(AZURE_COSMOS_DB_DATABASE);
        var azureCosmosDbUserContainerName = getEnvVar(AZURE_COSMOS_DB_USER_CONTAINER_NAME);
        var azureCosmosDbAuctionContainerName = getEnvVar(AZURE_COSMOS_DB_AUCTION_CONTAINER_NAME);
        var azureCosmosDbBidContainerName = getEnvVar(AZURE_COSMOS_DB_BID_CONTAINER_NAME);
        var azureCosmosDbQuestionContainerName = getEnvVar(AZURE_COSMOS_DB_QUESTION_CONTAINER_NAME);

        return new CosmosDbConfig(
                azureCosmosDbKey,
                azureCosmosDbUrl,
                azureCosmosDbDatabase,
                azureCosmosDbAuctionContainerName,
                azureCosmosDbUserContainerName,
                azureCosmosDbBidContainerName,
                azureCosmosDbQuestionContainerName);
    }

    public static final RedisConfig getAzureRedisConfig() {
        var azureRedisKey = getEnvVar(AZURE_REDIS_KEY);
        var azureRedisUrl = getEnvVar(AZURE_REDIS_URL);

        return new RedisConfig(azureRedisKey, azureRedisUrl);
    }

    private static String getEnvVar(String name, String defaultValue) {
        var value = System.getenv(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private static String getEnvVar(String name) {
        var value = System.getenv(name);
        if (value == null) {
            throw new RuntimeException("Missing environment variable: " + name);
        }
        return value;
    }
}
