package scc.cache;

import java.util.Optional;

public class NoOpCache implements Cache, AuctionCache, BidCache, MediaCache, QuestionCache, UserCache {

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public Optional<byte[]> getBytes(String key) {
        return Optional.empty();
    }

    @Override
    public String set(String key, String value) {
        return null;
    }

    @Override
    public String setBytes(String key, byte[] value) {
        return null;
    }

    @Override
    public Long expire(String key, int seconds) {
        return 0L;
    }

    @Override
    public Long del(String... keys) {
        return 0L;
    }

    @Override
    public void deleteAuction(String auctionId) { }

    @Override
    public void deleteUser(String userId) { }
}
