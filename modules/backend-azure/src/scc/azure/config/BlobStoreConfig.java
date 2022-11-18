package scc.azure.config;

public class BlobStoreConfig {
    public final String connectionString;
    public final String auctionContainer;
    public final String userContainer;

    public BlobStoreConfig(String connectionString, String auctionContainer, String userContainer) {
        this.connectionString = connectionString;
        this.auctionContainer = auctionContainer;
        this.userContainer = userContainer;
    }
}
