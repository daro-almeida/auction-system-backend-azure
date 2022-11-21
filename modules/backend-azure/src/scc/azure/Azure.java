package scc.azure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.MediaId;
import scc.MediaNamespace;
import scc.azure.config.CosmosDbConfig;
import scc.azure.config.RedisConfig;
import scc.utils.Hash;

public class Azure {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nX");

    public static LocalDateTime parseDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime.atZone(ZoneOffset.UTC));
    }

    public static String formatDateTime(ZonedDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime.withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String hashUserPassword(String password) {
        return Hash.of(password);
    }

    public static String mediaNamespaceToString(MediaNamespace namespace) {
        return switch (namespace) {
            case User -> "user";
            case Auction -> "auction";
            default -> throw new IllegalArgumentException("Unknown namespace: " + namespace);
        };
    }

    public static MediaNamespace mediaNamespaceFromString(String namespace) {
        return switch (namespace) {
            case "user" -> MediaNamespace.User;
            case "auction" -> MediaNamespace.Auction;
            default -> throw new IllegalArgumentException("Unknown namespace: " + namespace);
        };
    }

    public static String mediaIdToString(MediaId mediaId) {
        return mediaNamespaceToString(mediaId.getNamespace()) + "/" + mediaId.getId();
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
                // .directMode()
                .gatewayMode()
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        var dbClient = cosmosClient.getDatabase(config.dbName);
        return dbClient;
    }

    public static Jedis createJedis(RedisConfig config) {
        var jedis = new Jedis(config.url, 6380, true);
        jedis.auth(config.key);
        return jedis;
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

    public static ObjectMapper createObjectMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return objectMapper;
    }

}
