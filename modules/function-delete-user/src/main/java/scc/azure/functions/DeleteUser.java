package scc.azure.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import scc.azure.Azure;
import scc.azure.Cosmos;
import scc.azure.Redis;
import scc.azure.config.AzureEnv;
import scc.azure.config.MessageBusConfig;

/**
 * Azure Function to delete a user.
 * 
 * To delete a user its status must be changed to DELETED and all of its
 * auctions and bids must be invalidated from cache so that they can be
 * displayed
 */
public class DeleteUser {
    @FunctionName("deleteUser")
    public void deleteUser(
            @ServiceBusQueueTrigger(name = "userId", queueName = MessageBusConfig.QUEUE_DELETE_USER, connection = AzureEnv.AZURE_MESSAGE_BUS_CONNECTION_STRING) String userId,
            final ExecutionContext context) {
        context.getLogger().info("Closing auction: " + userId);

        var redisConfig = AzureEnv.getAzureRedisConfig();
        var cosmosConfig = AzureEnv.getAzureCosmosDbConfig();
        var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
        var userContainer = cosmosDb.getContainer(cosmosConfig.userContainer);
        try (var jedis = Azure.createJedis(redisConfig)) {
            var result = Cosmos.deleteUser(userContainer, userId);
            if (result.isOk()) {
                Redis.removeUser(jedis, userId);
                context.getLogger().info("User " + userId + " deleted");
            } else {
                context.getLogger().warning("User " + userId + " not deleted");
                context.getLogger().warning(result.error() + " " + result.errorMessage());
            }
        }
    }
}