package scc.azure.config;

public class MessageBusConfig {
    public static final String QUEUE_CLOSE_AUCTION = "close-auction";

    public final String connectionString;

    public MessageBusConfig(String connectionString) {
        this.connectionString = connectionString;
    }
}
