package scc.azure;

import java.util.Optional;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.azure.config.CosmosDbConfig;
import scc.azure.config.RedisConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.services.data.AuctionItem;
import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.services.data.ReplyItem;
import scc.utils.Hash;

public interface AzureUtils {
    public static String hashUserPassword(String password) {
        return Hash.of(password);
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

    public static AuctionItem auctionDaoToItem(AuctionDAO auction) {
        return auctionDaoToItem(auction, null);
    }

    public static AuctionItem auctionDaoToItem(AuctionDAO auction, BidDAO bid) {
        return new AuctionItem(
                auction.getId(),
                auction.getTitle(),
                auction.getDescription(),
                auction.getPictureId(),
                auction.getUserId(),
                auction.getEndTime(),
                auction.getStartingPrice(),
                Optional.ofNullable(bid).map(AzureUtils::bidDaoToItem),
                auction.getStatus());
    }

    public static BidItem bidDaoToItem(BidDAO bid) {
        return new BidItem(
                bid.getId(),
                bid.getUserId(),
                bid.getAuctionId(),
                bid.getAmount());
    }
}
