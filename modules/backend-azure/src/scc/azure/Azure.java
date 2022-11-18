package scc.azure;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.MediaId;
import scc.MediaNamespace;
import scc.azure.config.CosmosDbConfig;
import scc.azure.config.RedisConfig;
import scc.utils.Hash;

public class Azure {
    public static String hashUserPassword(String password) {
        return Hash.of(password);
    }

    public static String mediaNamespaceToString(MediaNamespace namespace) {
        return switch (namespace) {
            case User -> "user";
            case Auction -> "auction";
        };
    }

    public static MediaNamespace mediaNamespaceFromString(String namespace) {
        return switch (namespace) {
            case "user" -> MediaNamespace.User;
            case "auction" -> MediaNamespace.Auction;
            default -> throw new IllegalArgumentException("Invalid media namespace: " + namespace);
        };
    }

    public static String mediaIdToString(MediaId mediaId) {
        return mediaId.getNamespace().toString() + "/" + mediaId.getId();
    }

    public static MediaId mediaIdFromString(String mediaId) {
        var parts = mediaId.split("/", 2);
        if (parts.length != 2)
            throw new IllegalArgumentException("Invalid media id: " + mediaId);

        var namespace = mediaNamespaceFromString(parts[0]);
        var id = parts[1];
        return new MediaId(namespace, id);
    }

    public static CosmosDatabase createCosmosDatabase(CosmosDbConfig config) {
        var cosmosClient = new CosmosClientBuilder()
                .endpoint(config.dbUrl)
                .key(config.dbKey)
                .directMode()
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        var dbClient = cosmosClient.getDatabase(config.dbName);
        return dbClient;
    }

    public static JedisPool createJedisPool(RedisConfig config) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return new JedisPool(poolConfig, config.url, 6380, 1000, config.key, true);
    }

}
