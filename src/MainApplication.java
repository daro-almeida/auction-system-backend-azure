import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import scc.azure.AzureMonolithService;
import scc.azure.config.AzureMonolithConfig;
import scc.azure.config.BlobStoreConfig;
import scc.azure.config.CosmosDbConfig;
import scc.memory.MemoryAuctionService;
import scc.memory.MemoryMediaService;
import scc.memory.MemoryUserService;
import scc.resources.AuctionResource;
import scc.resources.ControlResource;
import scc.resources.MediaResource;
import scc.resources.UserResource;

public class MainApplication extends Application {
    private static final String ENV_BACKEND_KIND = "BACKEND_KIND";
    private static final String ENV_BACKEND_KIND_MEM = "memory";
    private static final String ENV_BACKEND_KIND_AZURE = "azure";

    private static final String ENV_AZURE_BLOB_STORAGE_CONNECTION_STRING = "AZURE_BLOB_STORAGE_CONNECTION_STRING";
    private static final String ENV_AZURE_BLOB_STORAGE_AUCTION_MEDIA_CONTAINER_NAME = "AZURE_BLOB_STORAGE_AUCTION_MEDIA_CONTAINER_NAME";
    private static final String ENV_AZURE_BLOB_STORAGE_USER_MEDIA_CONTAINER_NAME = "AZURE_BLOB_STORAGE_USER_MEDIA_CONTAINER_NAME";

    private static final String ENV_AZURE_COSMOS_DB_KEY = "AZURE_COSMOS_DB_KEY";
    private static final String ENV_AZURE_COSMOS_DB_URL = "AZURE_COSMOS_DB_URL";
    private static final String ENV_AZURE_COSMOS_DB_DATABASE = "AZURE_COSMOS_DB_DATABASE";
    private static final String ENV_AZURE_COSMOS_DB_USER_CONTAINER_NAME = "AZURE_COSMOS_DB_USER_CONTAINER_NAME";
    private static final String ENV_AZURE_COSMOS_DB_AUCTION_CONTAINER_NAME = "AZURE_COSMOS_DB_AUCTION_CONTAINER_NAME";
    private static final String ENV_AZURE_COSMOS_DB_BID_CONTAINER_NAME = "AZURE_COSMOS_DB_BID_CONTAINER_NAME";
    private static final String ENV_AZURE_COSMOS_DB_QUESTION_CONTAINER_NAME = "AZURE_COSMOS_DB_QUESTION_CONTAINER_NAME";

    private static final String ENV_AZURE_ENABLE_CACHING = "AZURE_ENABLE_CACHING";
    private static final String ENV_AZURE_ENABLE_CACHING_YES = "1";
    private static final String ENV_AZURE_ENABLE_CACHING_NO = "0";

    private static final String ENV_AZURE_REDIS_KEY = "AZURE_REDIS_KEY";
    private static final String ENV_AZURE_REDIS_URL = "AZURE_REDIS_URL";

    private final Set<Object> singletons = new HashSet<Object>();
    private final Set<Class<?>> resources = new HashSet<Class<?>>();

    public MainApplication() {
        resources.add(ControlResource.class);
        resources.add(GenericExceptionMapper.class);

        var backendKind = System.getenv(ENV_BACKEND_KIND);
        if (backendKind == null) {
            backendKind = ENV_BACKEND_KIND_MEM;
        }
        switch (backendKind) {
            case ENV_BACKEND_KIND_MEM:
                System.out.println("Using memory backend");
                var mediaService = new MemoryMediaService();
                var userService = new MemoryUserService(mediaService);
                var auctionService = new MemoryAuctionService(userService, mediaService);
                singletons.add(new MediaResource(mediaService));
                singletons.add(new UserResource(userService));
                singletons.add(new AuctionResource(auctionService));
                break;
            case ENV_BACKEND_KIND_AZURE:
                System.out.println("Using Azure backend");
                var azureBlobStorageConnectionString = getEnvVar(ENV_AZURE_BLOB_STORAGE_CONNECTION_STRING);
                var azureBlobStorageAuctionMediaContainerName = getEnvVar(
                        ENV_AZURE_BLOB_STORAGE_AUCTION_MEDIA_CONTAINER_NAME);
                var azureBlobStorageUserMediaContainerName = getEnvVar(
                        ENV_AZURE_BLOB_STORAGE_USER_MEDIA_CONTAINER_NAME);
                var azureCosmosDbKey = getEnvVar(ENV_AZURE_COSMOS_DB_KEY);
                var azureCosmosDbUrl = getEnvVar(ENV_AZURE_COSMOS_DB_URL);
                var azureCosmosDbDatabase = getEnvVar(ENV_AZURE_COSMOS_DB_DATABASE);
                var azureCosmosDbUserContainerName = getEnvVar(ENV_AZURE_COSMOS_DB_USER_CONTAINER_NAME);
                var azureCosmosDbAuctionContainerName = getEnvVar(ENV_AZURE_COSMOS_DB_AUCTION_CONTAINER_NAME);
                var azureCosmosDbBidContainerName = getEnvVar(ENV_AZURE_COSMOS_DB_BID_CONTAINER_NAME);
                var azureCosmosDbQuestionContainerName = getEnvVar(ENV_AZURE_COSMOS_DB_QUESTION_CONTAINER_NAME);

                var config = new AzureMonolithConfig(
                        new BlobStoreConfig(
                                azureBlobStorageConnectionString,
                                azureBlobStorageAuctionMediaContainerName,
                                azureBlobStorageUserMediaContainerName),
                        new CosmosDbConfig(
                                azureCosmosDbKey,
                                azureCosmosDbUrl,
                                azureCosmosDbDatabase,
                                azureCosmosDbAuctionContainerName,
                                azureCosmosDbUserContainerName,
                                azureCosmosDbBidContainerName,
                                azureCosmosDbQuestionContainerName));

                var azureEnableCaching = getEnvVar(ENV_AZURE_ENABLE_CACHING, ENV_AZURE_ENABLE_CACHING_NO);
                switch (azureEnableCaching) {
                    case ENV_AZURE_ENABLE_CACHING_YES:
                        var azureRedisKey = getEnvVar(ENV_AZURE_REDIS_KEY);
                        var azureRedisUrl = getEnvVar(ENV_AZURE_REDIS_URL);
                        config.enableCaching(azureRedisKey, azureRedisUrl);
                        break;
                    case ENV_AZURE_ENABLE_CACHING_NO:
                        break;
                    default:
                        throw new RuntimeException(
                                "Invalid value for environment variable " + ENV_AZURE_ENABLE_CACHING);
                }

                var service = new AzureMonolithService(config);
                singletons.add(new MediaResource(service));
                singletons.add(new UserResource(service));
                singletons.add(new AuctionResource(service));
                break;
            default:
                throw new RuntimeException("Unknown backend kind: " + backendKind);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
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
