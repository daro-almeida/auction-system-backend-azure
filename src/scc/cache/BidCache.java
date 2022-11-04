package scc.cache;

public interface BidCache {

    String BID_PREFIX = "bid:";

    void set(String bidId, String bidJson);
}
