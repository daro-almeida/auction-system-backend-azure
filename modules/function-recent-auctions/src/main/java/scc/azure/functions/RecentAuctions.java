package scc.azure.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import scc.azure.Azure;
import scc.azure.Redis;
import scc.azure.config.AzureEnv;
import scc.azure.dao.AuctionDAO;

import java.util.List;

/**
 * Azure Functions with HTTP Trigger.
 */
public class RecentAuctions {
        @FunctionName("reactToAuction")
        public void reactToAuction(
                        @CosmosDBTrigger(name = "auctions", databaseName = "scc2223dbd464", collectionName = "auctions", connectionStringSetting = "AZURE_COSMOS_DB_CONNECTION_STRING", createLeaseCollectionIfNotExists = true) List<String> auctions,
                        final ExecutionContext context) throws JsonMappingException, JsonProcessingException {
                context.getLogger().info("Processign auctions: " + auctions.size());

                var redisConfig = AzureEnv.getAzureRedisConfig();
                var mapper = Azure.createObjectMapper();
                try (var jedis = Azure.createJedis(redisConfig)) {
                        for (var auction : auctions) {
                                context.getLogger().info("Adding auction " + auction);
                                var auctionDao = mapper.readValue(auction, AuctionDAO.class);
                                context.getLogger().info("Icrementing auction " + auctionDao.getId());
                                Redis.pushRecentAuction(jedis, auctionDao.getId());
                        }
                }
        }

        public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
        }
}
