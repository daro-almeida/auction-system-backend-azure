package scc.cache.redis;

import scc.azure.config.RedisConfig;
import scc.cache.AuctionCache;

public class RedisAuctionCache implements AuctionCache {

    private final RedisCache redisCache;

    public RedisAuctionCache(RedisConfig config) {
        this.redisCache = RedisCache.getInstance(config);
    }

    @Override
    public void deleteAuction(String auctionId) {
        redisCache.del(AUCTION_PREFIX + auctionId);
    }
}
