package scc.azure.config;

public class CosmosDbConfig {
    public final String dbKey;
    public final String dbUrl;
    public final String dbName;
    public final String auctionContainer;
    public final String userContainer;
    public final String bidContainer;
    public final String questionContainer;

    public CosmosDbConfig(String dbKey, String dbUrl, String dbName, String auctionContainer, String userContainer,
            String bidContainer, String questionContainer) {
        this.dbKey = dbKey;
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.auctionContainer = auctionContainer;
        this.userContainer = userContainer;
        this.bidContainer = bidContainer;
        this.questionContainer = questionContainer;
    }

    @Override
    public String toString() {
        return "CosmosDbConfig [dbKey=" + dbKey + ", dbUrl=" + dbUrl + ", dbName=" + dbName + ", auctionContainer="
                + auctionContainer + ", userContainer=" + userContainer + ", bidContainer=" + bidContainer
                + ", questionContainer=" + questionContainer + "]";
    }
}