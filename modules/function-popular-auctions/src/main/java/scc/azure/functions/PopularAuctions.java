package scc.azure.functions;

import com.azure.cosmos.implementation.PreconditionFailedException;
import com.azure.cosmos.models.CosmosPatchItemRequestOptions;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import scc.azure.Azure;
import scc.azure.Redis;
import scc.azure.config.AzureEnv;
import scc.azure.dao.BidDAO;

import java.util.List;

/**
 * Azure Functions with HTTP Trigger.
 */
public class PopularAuctions {
        @FunctionName("reactToBid")
        public void reactToBid(
                        @CosmosDBTrigger(name = "bids", databaseName = "scc2223dbd464", collectionName = "bids", connectionStringSetting = "AZURE_COSMOS_DB_CONNECTION_STRING", createLeaseCollectionIfNotExists = true) List<String> bids,
                        final ExecutionContext context) throws JsonMappingException, JsonProcessingException {
                context.getLogger().info("Processign bids: " + bids.size());

                var redisConfig = AzureEnv.getAzureRedisConfig();
                var mapper = Azure.createObjectMapper();
                try (var jedis = Azure.createJedis(redisConfig)) {
                        for (var bid : bids) {
                                var bidDao = mapper.readValue(bid, BidDAO.class);
                                context.getLogger().info("Icrementing auction " + bidDao.getAuctionId());
                                Redis.incrementPopularAuction(jedis, bidDao.getAuctionId());
                        }
                }
        }

        @FunctionName("updatePopularAuctions")
        public void updatePopularAuctions(
                        @TimerTrigger(name = "timerInfo", schedule = "0 */2 * * * *") String timerInfo,
                        final ExecutionContext context) {
                context.getLogger().info("Updating most popular auctions");
                var redisConfig = AzureEnv.getAzureRedisConfig();
                try (var jedis = Azure.createJedis(redisConfig)) {
                        Redis.updatePopularAuctions(jedis);
                }
        }

        public static void main(String[] args) {
                var cosmosConfig = AzureEnv.getAzureCosmosDbConfig();
                var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
                var container = cosmosDb.getContainer("questions");
                var questionId = "d1eab198-de64-406f-9765-614510c659d0";

                var ops = CosmosPatchOperations.create()
                                .set("/reply", "sup4");
                var opts = new CosmosPatchItemRequestOptions()
                                .setFilterPredicate("FROM questions q WHERE q.reply = 'sup2'");
                try {
                        container.patchItem(questionId, new PartitionKey(questionId), ops, opts, null);
                } catch (PreconditionFailedException e) {
                        System.out.println("Precondition failed, but that is not imporant for us");
                }
        }

}
