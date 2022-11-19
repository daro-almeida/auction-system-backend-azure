package scc.azure.functions;

import java.time.LocalDateTime;
import java.util.List;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import scc.azure.Azure;
import scc.azure.AzureLogic;
import scc.azure.Cosmos;
import scc.azure.Redis;
import scc.azure.config.AzureEnv;
import scc.azure.dao.AuctionDAO;

/**
 * Azure Functions with HTTP Trigger.
 */
public class AuctionsAboutToClose {
    @FunctionName("updateAuctionsAboutToCloseReactToInsert")
    public void updateAuctionsAboutToCloseReactToInsert(
            @CosmosDBTrigger(name = "auctions", databaseName = "scc2223dbd464", collectionName = "auctions", connectionStringSetting = "AZURE_COSMOS_DB_CONNECTION_STRING", createLeaseCollectionIfNotExists = true) List<AuctionDAO> auctions,
            final ExecutionContext context) {
        context.getLogger().info("Processign auctions: " + auctions.size());

        var redisConfig = AzureEnv.getAzureRedisConfig();
        try (var jedis = Azure.createJedis(redisConfig)) {
            var now = LocalDateTime.now();
            for (var auction : auctions) {
                var endTime = Azure.parseDateTime(auction.getEndTime());
                if (auction.getStatus() != AuctionDAO.Status.OPEN)
                    continue;
                if (!AzureLogic.isAuctionEndTimeAboutToClose(now, endTime))
                    continue;

                context.getLogger().info("Adding auction " + auction.getId() + " to auctions about to close");
                Redis.addAuctionAboutToClose(jedis, new Redis.AuctionAboutToClose(auction.getId(), endTime));
            }
        }
    }

    @FunctionName("updateAuctionsAboutToClose")
    public void updateAuctionsAboutToClose(
            @TimerTrigger(name = "timerInfo", schedule = "0 */2 * * * *") String timerInfo,
            final ExecutionContext context) {
        context.getLogger().info("Updating auctions about to close");
        var cosmosConfig = AzureEnv.getAzureCosmosDbConfig();
        var redisConfig = AzureEnv.getAzureRedisConfig();
        var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
        var container = cosmosDb.getContainer(cosmosConfig.auctionContainer);
        var auctionDaos = Cosmos.listAuctionsAboutToClose(container).value();
        var auctionsAboutToClose = auctionDaos.stream()
                .map(dao -> new Redis.AuctionAboutToClose(dao.getId(), Azure.parseDateTime(dao.getEndTime())))
                .toList();
        try (var jedis = Azure.createJedis(redisConfig)) {
            Redis.setAuctionsAboutToClose(jedis, auctionsAboutToClose);
        }
    }
}
