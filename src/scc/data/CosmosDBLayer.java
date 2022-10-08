package scc.data;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.database.UserDAO;

public abstract class CosmosDBLayer {
    private static final String CONNECTION_URL = "https://scc2223ddb.documents.azure.com:443/";
    private static final String DB_KEY = "sidbO47aj4HzQkgQmkkfTqubuqGX1EavpYOiBt73EkKeR5zRBXPaDqEm8nD3BMLwZdZRCNmKRkEQuPYmOlmYKg==";
    private static final String DB_NAME = "scc2223container";

    private static CosmosClient clientInstance;
    private static CosmosDatabase dbInstance;
    private final CosmosClient client;
    protected final CosmosDatabase db;

    public CosmosDBLayer() {
        this.client = getClientInstance();
        this.db = getDbInstance();
    }

    private static synchronized CosmosClient getClientInstance() {
        if (clientInstance != null)
            return clientInstance;

        clientInstance = new CosmosClientBuilder()
                .endpoint(CONNECTION_URL)
                .key(DB_KEY)
                //.directMode()
                .gatewayMode()
                // replace by .directMode() for better performance
                .consistencyLevel(ConsistencyLevel.SESSION)
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        return clientInstance;
    }

    private static synchronized CosmosDatabase getDbInstance() {
        if (dbInstance != null)
            return dbInstance;
        dbInstance = getClientInstance().getDatabase(DB_NAME);
        return dbInstance;
    }

    public void close() {
        client.close();
    }
}
