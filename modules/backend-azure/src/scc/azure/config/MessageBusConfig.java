package scc.azure.config;

public class MessageBusConfig {
    public static final String QUEUE_CLOSE_AUCTION = "close-auction";
    public static final String QUEUE_DELETE_USER = "delete-user";

    public final String connectionString;

    public MessageBusConfig(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public String toString() {
        return "MessageBusConfig [connectionString=" + connectionString + "]";
    }
}
