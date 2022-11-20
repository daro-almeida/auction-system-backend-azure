package scc.azure.functions;

import com.azure.cosmos.models.CosmosContainerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import scc.azure.Azure;
import scc.azure.Cosmos;
import scc.azure.Redis;
import scc.azure.config.AzureEnv;
import scc.azure.dao.AuctionDAO;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Azure Functions with HTTP Trigger.
 */
public class RecentAuctions {
        @FunctionName("reactToAuction")
        public void reactToAuction(
                        @CosmosDBTrigger(name = "auctions", databaseName = "scc2223dbd464", collectionName = "auctions", connectionStringSetting = "AZURE_COSMOS_DB_CONNECTION_STRING", createLeaseCollectionIfNotExists = true) List<AuctionDAO> auctions,
                        final ExecutionContext context) throws JsonMappingException, JsonProcessingException {
                context.getLogger().info("Processing auctions: " + auctions.size());

                var redisConfig = AzureEnv.getAzureRedisConfig();
                try (var jedis = Azure.createJedis(redisConfig)) {
                        for (var auction : auctions) {
                                context.getLogger().info("Adding auction " + auction);
                                context.getLogger().info("Incrementing auction " + auction.getId());
                                Redis.pushRecentAuction(jedis, auction.getId());
                        }
                }
        }

        public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
                var date = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nX")
                                .format(java.time.LocalDateTime.now().atZone(ZoneOffset.UTC));
                System.out.println(date);

                // var cosmosConfig = AzureEnv.getAzureCosmosDbConfig();
                // var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
                // var container = cosmosDb.getContainer("auctions");
                // var aboutToClose = Cosmos.listAuctionsAboutToClose(container);
                // for (var auction : aboutToClose.value()) {
                // System.out.println(auction.getId());
                // }

                var redisConfig = AzureEnv.getAzureRedisConfig();
                var cosmosConfig = AzureEnv.getAzureCosmosDbConfig();
                var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
                var containers = new String[] { cosmosConfig.bidContainer,
                                cosmosConfig.auctionContainer,
                                cosmosConfig.userContainer, cosmosConfig.questionContainer };
                var jedis = Azure.createJedis(redisConfig);

                // var bidOpt = Cosmos.getBid(cosmosDb.getContainer("bids"),
                // "5dc52551-a58a-4f7d-aed1-b79238941c2c");
                // if (bidOpt.isEmpty()) {
                // System.out.println("Bid not found");
                // } else {
                // var bid = bidOpt.get();
                // System.out.println(bid);
                // }

                // Recreate all containers
                for (var name : containers) {
                        cosmosDb.getContainer(name).delete();
                        cosmosDb.createContainer(new CosmosContainerProperties(name, "/id"));
                }
        }
}
