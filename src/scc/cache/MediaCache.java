package scc.cache;

public interface MediaCache {

    String USER_MEDIA_PREFIX = "userMedia:";
    String AUCTION_MEDIA_PREFIX = "auctionMedia:";

    void setUserMedia(String hash, byte[] contents);

    void setAuctionMedia(String hash, byte[] contents);
}
