package scc.azure.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import scc.azure.Azure;
import scc.azure.Redis;
import scc.azure.config.AzureEnv;

/**
 * Azure Functions with HTTP Trigger.
 */
public class AuctionsAboutToClose {
    @FunctionName("updateAuctionsAboutToClose")
    public void updateAuctionsAboutToClose(
            @TimerTrigger(name = "timerInfo", schedule = "0 */2 * * * *") String timerInfo,
            final ExecutionContext context) {
        context.getLogger().info("Updating auctions about to close");
        var redisConfig = AzureEnv.getAzureRedisConfig();
        try (var jedis = Azure.createJedis(redisConfig)) {
            Redis.updatePopularAuctions(jedis);
        }
    }
}
