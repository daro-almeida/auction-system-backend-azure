package scc.azure.functions;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import scc.azure.Azure;
import scc.azure.AzureEnv;
import scc.azure.config.MessageBusConfig;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    @FunctionName("test-receiver")
    public void testReceiver(
            @ServiceBusQueueTrigger(name = "message", queueName = MessageBusConfig.QUEUE_CLOSE_AUCTION, connection = AzureEnv.AZURE_MESSAGE_BUS_CONNECTION_STRING) String message,
            final ExecutionContext context) {
        context.getLogger().info("Received message: " + message);
        System.out.println("Received message: " + message);
    }

    public static void main(String[] args) {
        var messageBusConfig = AzureEnv.getAzureMessageBusConfig();
        ServiceBusSenderClient sender = new ServiceBusClientBuilder()
                .connectionString(messageBusConfig.connectionString)
                .sender()
                .queueName(MessageBusConfig.QUEUE_CLOSE_AUCTION)
                .buildClient();

        System.out.println("Sending message");
        sender.sendMessage(
                new ServiceBusMessage("Hello world!")
                        .setScheduledEnqueueTime(
                                LocalDateTime.now().plus(Duration.ofSeconds(3)).atOffset(ZoneOffset.UTC)));

        sender.close();
    }
}
