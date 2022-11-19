package scc.azure.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import scc.azure.Azure;
import scc.azure.Cosmos;
import scc.azure.Redis;
import scc.azure.config.AzureEnv;
import scc.azure.config.MessageBusConfig;

public class CloseAuctions {
    @FunctionName("closeAuctions")
    public void closeAuctions(
            @ServiceBusQueueTrigger(name = "auctionId", queueName = MessageBusConfig.QUEUE_CLOSE_AUCTION, connection = AzureEnv.AZURE_MESSAGE_BUS_CONNECTION_STRING) String auctionId,
            final ExecutionContext context) {
        context.getLogger().info("Closing auction: " + auctionId);

        var redisConfig = AzureEnv.getAzureRedisConfig();
        var cosmosConfig = AzureEnv.getAzureCosmosDbConfig();
        var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
        var auctionContainer = cosmosDb.getContainer(cosmosConfig.auctionContainer);
        try (var jedis = Azure.createJedis(redisConfig)) {
            var result = Cosmos.closeAuction(auctionContainer, auctionId);
            if (result.isOk()) {
                context.getLogger().info("Auction closed: " + auctionId);
                Redis.setAuction(jedis, result.value());
                Redis.removeAuctionAboutToClose(jedis, auctionId);
            } else {
                context.getLogger().info(
                        "Auction not closed: " + auctionId + " - " + result.error() + " - " + result.errorMessage());
            }
        }
    }
}
