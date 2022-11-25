package scc.kube.config;

public class MongoConfig {
    public final String connectionUri;
    public final String databaseName;
    public final String auctionCollection;
    public final String bidCollection;
    public final String questionCollection;
    public final String userCollection;

    public MongoConfig(String connectionUri, String databaseName, String auctionCollection, String bidCollection,
            String questionCollection, String userCollection) {
        this.connectionUri = connectionUri;
        this.databaseName = databaseName;
        this.auctionCollection = auctionCollection;
        this.bidCollection = bidCollection;
        this.questionCollection = questionCollection;
        this.userCollection = userCollection;
    }

    @Override
    public String toString() {
        return "MongoConfig [connectionUri=" + connectionUri + ", databaseName=" + databaseName + ", auctionCollection="
                + auctionCollection + ", bidCollection=" + bidCollection + ", questionCollection=" + questionCollection
                + ", userCollection=" + userCollection + "]";
    }
}
