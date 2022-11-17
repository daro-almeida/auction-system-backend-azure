package scc.azure.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import scc.azure.AuctionDB;
import scc.azure.AzureUtils;
import scc.azure.cache.RedisCache;
import scc.utils.SccEnv;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class PopularAuctions {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it
     * using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("trigger")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Updating cache with popular auctions");

        var redisConfig = SccEnv.getAzureRedisConfig();
        System.out.println("Redis config: " + redisConfig);
        var jedisPool = AzureUtils.createJedisPool(redisConfig);
        var cache = new RedisCache(jedisPool);
        var client = jedisPool.getResource();
        client.sadd("test-set", request.getQueryParameters().get("name"));

        return request.createResponseBuilder(HttpStatus.OK).body("Everything is OK").build();
    }

    public static void main(String[] args) {
        var redisConfig = SccEnv.getAzureRedisConfig();
        var cosmosConfig = SccEnv.getAzureCosmosDbConfig();
        System.out.println("Redis config: " + redisConfig);

        var cosmosDb = AzureUtils.createCosmosDatabase(cosmosConfig);
        var container = cosmosDb.getContainer(cosmosConfig.auctionContainer);
    }
}
