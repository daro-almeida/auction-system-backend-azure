package scc.cache;

public interface AuctionCache {

    String AUCTION_PREFIX = "auction:";

    /**
     * Deletes an auction entry with given key from the cache
     * @param auctionId identifier of the auction
     */
    void deleteAuction(String auctionId);

    void set(String auctionId, String auctionJson);
}
