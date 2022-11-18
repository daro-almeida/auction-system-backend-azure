package scc.azure.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import scc.azure.Azure;
import scc.azure.AzureEnv;

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
        return request.createResponseBuilder(HttpStatus.OK).body("Everything is OK").build();
    }

    public static void main(String[] args) {
        var redisConfig = AzureEnv.getAzureRedisConfig();
        var cosmosConfig = AzureEnv.getAzureCosmosDbConfig();
        System.out.println("Redis config: " + redisConfig);

        var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
        var container = cosmosDb.getContainer(cosmosConfig.auctionContainer);
    }
}
