package scc.kube;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import scc.AppLogic;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.QuestionDao;
import scc.kube.dao.UserDao;

public class Redis {
    public static final int TTL_DAO = 60 * 60;
    public static final int TTL_SESSION = 30 * 60;

    public static final String PREFIX_AUCTION = "auction:";
    public static final String PREFIX_BID = "bid:";
    public static final String PREFIX_QUESTION = "question:";
    public static final String PREFIX_USER = "user:";
    public static final String PREFIX_USER_TOKEN = "user-token:";
    public static final String PREFIX_TOP_BID = "top-bid:";
    public static final String PREFIX_AUCTION_BIDS = "auction-bids:";
    public static final String PREFIX_AUCTION_QUESTIONS = "auction-questions:";
    public static final String PREFIX_USER_AUCTIONS = "user-auctions:";
    public static final String PREFIX_USER_FOLLOWED_AUCTIONS = "user-followed-auctions:";
    public static final String PREFIX_USER_DISPLAY_NAME = "user-display-name:";
    public static final String PREFIX_USERNAME_TO_ID = "username-to-id:";

    public static final String KEY_AUCTIONS_ABOUNT_TO_CLOSE = "auctions-about-to-close";
    public static final String KEY_RECENT_AUCTIONS = "recent-auctions";
    public static final String KEY_POPULAR_AUCTIONS = "popular-auctions";
    public static final String KEY_POPULAR_AUCTIONS_RANKING = "popular-auctions-ranking";

    private static final int MAX_AUCTION_BIDS = 256;

    /* ------------------------- Auction ------------------------- */

    @WithSpan
    public static void setAuction(Jedis jedis, AuctionDao auctionDao) {
        var key = PREFIX_AUCTION + auctionDao.id;
        setDao(jedis, key, auctionDao);
    }

    @WithSpan
    public static void setAuctionMany(Jedis jedis, Collection<AuctionDao> auctionDaos) {
        try (var pipeline = jedis.pipelined()) {
            for (var auctionDao : auctionDaos) {
                var key = PREFIX_AUCTION + auctionDao.id;
                setDao(pipeline, key, auctionDao);
            }
        }
    }

    @WithSpan
    public static void unsetAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        unsetDao(jedis, key);
    }

    @WithSpan
    public static AuctionDao getAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        return getDao(jedis, key, AuctionDao.class);
    }

    @WithSpan
    public static HashMap<ObjectId, AuctionDao> getAuctionMany(Jedis jedis, Collection<ObjectId> auctionIds) {
        return getDaoMany(jedis, PREFIX_AUCTION, auctionIds, AuctionDao.class);
    }

    @WithSpan
    public static void pushAuctionBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        jedis.lpush(key, bidId.toString());
        jedis.ltrim(key, 0, MAX_AUCTION_BIDS - 1);
        jedis.expire(key, TTL_DAO);
    }

    @WithSpan
    public static List<ObjectId> getAuctionBids(Jedis jedis, ObjectId auctionId, int skip, int limit) {
        if (limit > MAX_AUCTION_BIDS)
            return null;
        var key = PREFIX_AUCTION_BIDS + auctionId;
        var bidIds = jedis.lrange(key, skip, skip + limit - 1).stream().map(ObjectId::new).toList();
        if (bidIds.size() < limit)
            return null;
        return bidIds;
    }

    @WithSpan
    public static void setAuctionTopBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_TOP_BID + auctionId;
        jedis.set(key, bidId.toHexString());
    }

    @WithSpan
    public static ObjectId getAuctionTopBid(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_TOP_BID + auctionId;
        var bidId = jedis.get(key);
        if (bidId == null)
            return null;
        return new ObjectId(bidId);
    }

    @WithSpan
    public static Map<ObjectId, ObjectId> getAuctionTopBidMany(Jedis jedis, Collection<ObjectId> auctionIds) {
        var responses = new HashMap<ObjectId, Response<String>>();
        try (var pipeline = jedis.pipelined()) {
            for (var auctionId : auctionIds) {
                var key = PREFIX_TOP_BID + auctionId;
                responses.put(auctionId, pipeline.get(key));
            }
        }
        var map = new HashMap<ObjectId, ObjectId>();
        for (var entry : responses.entrySet()) {
            var bidId = entry.getValue().get();
            if (bidId != null)
                map.put(entry.getKey(), new ObjectId(bidId));
        }
        return map;
    }

    /* ------------------------- Bid ------------------------- */

    @WithSpan
    public static void setBid(Jedis jedis, BidDao bidDao) {
        setBidMany(jedis, List.of(bidDao));
    }

    @WithSpan
    public static void setBidMany(Jedis jedis, Collection<BidDao> bidDaos) {
        try (var pipeline = jedis.pipelined()) {
            for (var bidDao : bidDaos) {
                var key = PREFIX_BID + bidDao.id;
                setDao(pipeline, key, bidDao);
            }
        }
    }

    @WithSpan
    public static void unsetBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        unsetDao(jedis, key);
    }

    @WithSpan
    public static BidDao getBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        return getDao(jedis, key, BidDao.class);
    }

    @WithSpan
    public static HashMap<ObjectId, BidDao> getBidMany(Jedis jedis, Collection<ObjectId> bidIds) {
        return getDaoMany(jedis, PREFIX_BID, bidIds, BidDao.class);
    }

    /* ------------------------- Question ------------------------- */

    @WithSpan
    public static void setQuestion(Jedis jedis, QuestionDao questionDao) {
        var key = PREFIX_QUESTION + questionDao.id;
        setDao(jedis, key, questionDao);
    }

    @WithSpan
    public static void unsetQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        unsetDao(jedis, key);
    }

    @WithSpan
    public static QuestionDao getQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        return getDao(jedis, key, QuestionDao.class);
    }

    @WithSpan
    public static HashMap<ObjectId, QuestionDao> getQuestionMany(Jedis jedis, Collection<ObjectId> questionIds) {
        return getDaoMany(jedis, PREFIX_QUESTION, questionIds, QuestionDao.class);
    }

    /* ------------------------- User ------------------------- */

    @WithSpan
    public static void setUser(Jedis jedis, UserDao userDao) {
        setUserMany(jedis, List.of(userDao));
    }

    @WithSpan
    public static void setUserMany(Jedis jedis, Collection<UserDao> userDaos) {
        try (var pipeline = jedis.pipelined()) {
            for (var userDao : userDaos) {
                var daoKey = PREFIX_USER + userDao.id;
                setDao(pipeline, daoKey, userDao);
                var usernameKey = PREFIX_USERNAME_TO_ID + userDao.username;
                pipeline.set(usernameKey, userDao.id.toHexString());
            }
        }
    }

    @WithSpan
    public static void unsetUser(Jedis jedis, ObjectId userId) {
        var key = PREFIX_USER + userId;
        unsetDao(jedis, key);
    }

    @WithSpan
    public static UserDao getUser(Jedis jedis, ObjectId userId) {
        var key = PREFIX_USER + userId;
        return getDao(jedis, key, UserDao.class);
    }

    @WithSpan
    public static Map<ObjectId, UserDao> getUserMany(Jedis jedis, Collection<ObjectId> userIds) {
        return getDaoMany(jedis, PREFIX_USER, userIds, UserDao.class);
    }

    @WithSpan
    public static ObjectId getUserIdFromUsername(Jedis jedis, String username) {
        var key = PREFIX_USERNAME_TO_ID + username;
        var userId = jedis.get(key);
        if (userId == null)
            return null;
        return new ObjectId(userId);
    }

    @WithSpan
    private static void setUserIdFromUsernameMany(Jedis jedis, Map<String, ObjectId> userIds) {
        try (var pipeline = jedis.pipelined()) {
            for (var entry : userIds.entrySet()) {
                var key = PREFIX_USERNAME_TO_ID + entry.getKey();
                pipeline.set(key, entry.getValue().toHexString());
                pipeline.expire(key, TTL_DAO);
            }
        }
    }

    @WithSpan
    private static void setUserIdFromUsername(Jedis jedis, String username, ObjectId userId) {
        setUserIdFromUsernameMany(jedis, Map.of(username, userId));
    }

    @WithSpan
    public static void setUserAuctions(Jedis jedis, ObjectId userId, Collection<ObjectId> auctionIds) {
        var key = PREFIX_USER_AUCTIONS + userId;
        jedis.del(key);
        jedis.rpush(key, auctionIds.stream().map(ObjectId::toString).toArray(String[]::new));
        jedis.expire(key, TTL_DAO);
    }

    @WithSpan
    public static void pushUserAuction(Jedis jedis, ObjectId userId, ObjectId auctionId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        jedis.rpush(key, auctionId.toString());
        jedis.expire(key, TTL_DAO);
    }

    @WithSpan
    public static List<ObjectId> getUserAuctions(Jedis jedis, ObjectId userId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        var auctionIds = jedis.lrange(key, 0, -1).stream().map(ObjectId::new).toList();
        return auctionIds;
    }

    @WithSpan
    public static void addUserFollowedAuction(Jedis jedis, ObjectId userId, ObjectId auctionId) {
        addUserFollowedAuctionMany(jedis, userId, List.of(auctionId));
    }

    @WithSpan
    public static void addUserFollowedAuctionMany(Jedis jedis, ObjectId userId, Collection<ObjectId> auctionIds) {
        try (var pipeline = jedis.pipelined()) {
            for (var auctionId : auctionIds) {
                var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
                jedis.sadd(key, auctionId.toString());
                jedis.expire(key, TTL_DAO);
            }
        }
    }

    @WithSpan
    public static List<ObjectId> getUserFollowedAuctions(Jedis jedis, ObjectId userId) {
        // TODO: use ordered set here
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        var auctionIds = jedis.smembers(key).stream().map(ObjectId::new).toList();
        return auctionIds;
    }

    @WithSpan
    public static void setUserDisplayName(Jedis jedis, ObjectId userId, String displayName) {
        setUserDisplayNameMany(jedis, Map.of(userId, displayName));
    }

    @WithSpan
    public static void setUserDisplayNameMany(Jedis jedis, Map<ObjectId, String> displayNames) {
        try (var pipeline = jedis.pipelined()) {
            for (var entry : displayNames.entrySet()) {
                var key = PREFIX_USER_DISPLAY_NAME + entry.getKey();
                jedis.set(key, entry.getValue());
                jedis.expire(key, TTL_DAO);
            }
        }
    }

    @WithSpan
    public static String getUserDisplayName(Jedis jedis, ObjectId userId) {
        var key = PREFIX_USER_DISPLAY_NAME + userId;
        return jedis.get(key);
    }

    @WithSpan
    public static Map<ObjectId, String> getUserDisplayNameMany(Jedis jedis, Iterable<ObjectId> userIds) {
        var map = new HashMap<ObjectId, String>();
        for (var userId : userIds) {
            var key = PREFIX_USER_DISPLAY_NAME + userId;
            var displayName = jedis.get(key);
            if (displayName != null)
                map.put(userId, displayName);
        }
        return map;
    }

    /* ------------------------- Session ------------------------- */

    @WithSpan
    public static void setSession(Jedis jedis, String username, String token) {
        var key = PREFIX_USER_TOKEN + token;
        jedis.setex(key, TTL_SESSION, username);
    }

    @WithSpan
    public static String getSession(Jedis jedis, String token) {
        var key = PREFIX_USER_TOKEN + token;
        var username = jedis.get(key);
        if (username == null)
            return null;
        return username;
    }

    @WithSpan
    public static void removeSession(Jedis jedis, String token) {
        var key = PREFIX_USER_TOKEN + token;
        jedis.del(key);
    }

    /*
     * ------------------------- Recent Auction Tracking -------------------------
     */

    @WithSpan
    public static void pushRecentAuction(Jedis jedis, ObjectId auctionId) {
        jedis.lpush(KEY_RECENT_AUCTIONS, auctionId.toHexString());
        jedis.ltrim(KEY_RECENT_AUCTIONS, 0, AppLogic.MAX_RECENT_AUCTIONS - 1);
    }

    @WithSpan
    public static List<ObjectId> getRecentAuctionIds(Jedis jedis) {
        var auctionIds = jedis.lrange(KEY_RECENT_AUCTIONS, 0, AppLogic.MAX_RECENT_AUCTIONS - 1)
                .stream().map(ObjectId::new).toList();
        return auctionIds;
    }

    /*
     * ---------------------- Soon To Close Auction Tracking -------------------
     */

    @WithSpan
    public static void setSoonToCloseAuctions(Jedis jedis, Collection<ObjectId> auctionIds) {
        jedis.del(KEY_AUCTIONS_ABOUNT_TO_CLOSE);
        jedis.rpush(KEY_AUCTIONS_ABOUNT_TO_CLOSE, auctionIds.stream().map(ObjectId::toString).toArray(String[]::new));
        jedis.expire(KEY_AUCTIONS_ABOUNT_TO_CLOSE, TTL_DAO);
    }

    @WithSpan
    public static List<ObjectId> getSoonToCloseAuctionIds(Jedis jedis) {
        var auctionIds = jedis.lrange(KEY_AUCTIONS_ABOUNT_TO_CLOSE, 0, -1)
                .stream().map(ObjectId::new).toList();
        return auctionIds;
    }

    /*
     * ----------------------- Auction Popularity Tracking -----------------------
     */

    @WithSpan
    public static List<ObjectId> getPopularAuctions(Jedis jedis) {
        var key = KEY_POPULAR_AUCTIONS;
        return jedis.lrange(key, 0, -1).stream().map(ObjectId::new).toList();
    }

    @WithSpan
    public static void updatePopularAuctions(Jedis jedis) {
        var mostPopular = jedis.zrevrange(KEY_POPULAR_AUCTIONS_RANKING, 0, AppLogic.MAX_MOST_POPULAR_AUCTIONS);
        if (mostPopular.size() > 0) {
            jedis.lpush(KEY_POPULAR_AUCTIONS, mostPopular.toArray(new String[mostPopular.size()]));
            jedis.ltrim(KEY_POPULAR_AUCTIONS, 0, AppLogic.MAX_MOST_POPULAR_AUCTIONS - 1);
        }
    }

    @WithSpan
    public static void incrementPopularAuction(Jedis jedis, ObjectId auctionId) {
        jedis.zincrby(KEY_POPULAR_AUCTIONS_RANKING, 1, auctionId.toHexString());
    }
    /* ------------------------- Internal ------------------------- */

    private static <T> void setDao(Jedis jedis, String key, T dao) {
        jedis.set(key, KubeSerde.toJson(dao));
        jedis.expire(key, TTL_DAO);
    }

    private static <T> void setDao(Pipeline jedis, String key, T dao) {
        jedis.set(key, KubeSerde.toJson(dao));
        jedis.expire(key, TTL_DAO);
    }

    private static void unsetDao(Jedis jedis, String key) {
        jedis.del(key);
    }

    private static <T> T getDao(Jedis jedis, String key, Class<T> clazz) {
        var json = jedis.get(key);
        if (json == null)
            return null;
        return KubeSerde.fromJson(json, clazz);
    }

    private static <T> HashMap<ObjectId, T> getDaoMany(
            Jedis jedis,
            String prefix,
            Collection<ObjectId> ids,
            Class<T> clazz) {
        var bidResponses = new HashMap<ObjectId, Response<String>>(ids.size());
        try (var pipeline = jedis.pipelined()) {
            for (var id : ids) {
                var key = PREFIX_BID + id;
                var response = pipeline.get(key);
                bidResponses.put(id, response);
            }
        }

        var daos = new HashMap<ObjectId, T>(ids.size());
        for (var entry : bidResponses.entrySet()) {
            var bidId = entry.getKey();
            var response = entry.getValue();
            if (response.get() == null)
                continue;
            var dao = KubeSerde.fromJson(response.get(), clazz);
            daos.put(bidId, dao);
        }

        return daos;
    }
}