package scc.azure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;

import scc.azure.config.MessageBusConfig;

public class MessageBus {
    public static void sendCloseAuction(String auctionId, LocalDateTime endTime) {
        var messageBusConfig = AzureEnv.getAzureMessageBusConfig();
        try (var sender = new ServiceBusClientBuilder()
                .connectionString(messageBusConfig.connectionString)
                .sender()
                .queueName(MessageBusConfig.QUEUE_CLOSE_AUCTION)
                .buildClient()) {
            var message = new ServiceBusMessage(auctionId)
                    .setScheduledEnqueueTime(endTime.atOffset(ZoneOffset.UTC));
            sender.sendMessage(message);
        }
    }
}
