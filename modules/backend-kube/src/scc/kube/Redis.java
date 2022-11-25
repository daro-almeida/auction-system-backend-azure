package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import scc.AppLogic;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.UserDao;
import scc.kube.dao.QuestionDao;

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

    public static final String KEY_AUCTIONS_ABOUNT_TO_CLOSE = "auctions-about-to-close";
    public static final String KEY_RECENT_AUCTIONS = "recent-auctions";
    public static final String KEY_POPULAR_AUCTIONS = "popular-auctions";
    public static final String KEY_POPULAR_AUCTIONS_RANKING = "popular-auctions-ranking";

    private static final int MAX_AUCTION_BIDS_LIST_SIZE = 256;

    private static final ObjectMapper mapper = Kube.createObjectMapper();

    /* ------------------------- Auction DAO ------------------------- */

    /**
     * Get the auction DAO from the cache.
     * 
     * @param jedis     The Redis client.
     * @param auctionId The auction id.
     * @return The auction DAO or null if not found.
     */
    public static AuctionDao getAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        return getDao(jedis, key, AuctionDao.class);
    }

    /**
     * Set the auction DAO in the cache.
     * 
     * 
     * @param jedis      The Redis client.
     * @param AuctionDao The auction DAO.
     */
    public static void setAuction(Jedis jedis, AuctionDao AuctionDao) {
        var key = PREFIX_AUCTION + AuctionDao.getId();
        setDao(jedis, key, AuctionDao);
    }

    /**
     * Delete the auction DAO from the cache.
     * 
     * @param jedis     The Redis client.
     * @param auctionId The auction id.
     */
    public static void removeAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        removeDao(jedis, key);
    }

    /* ------------------------- Bid DAO ------------------------- */

    /**
     * Get the bid DAO from the cache.
     * 
     * @param jedis The Redis client.
     * @param bidId The bid id.
     * @return The bid DAO or null if not found.
     */
    public static BidDao getBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        return getDao(jedis, key, BidDao.class);
    }

    public static HashMap<ObjectId, BidDao> getBidMany(Jedis jedis, List<ObjectId> bidIds) {
        var bidResponses = new HashMap<ObjectId, Response<String>>(bidIds.size());
        try (var pipeline = jedis.pipelined()) {
            for (var bidId : bidIds) {
                var bidKey = PREFIX_BID + bidId;
                bidResponses.put(bidId, pipeline.get(bidKey));
            }
        }

        var bids = new HashMap<ObjectId, BidDao>(bidIds.size());
        for (var entry : bidResponses.entrySet()) {
            var bidId = entry.getKey();
            var bidStr = entry.getValue().get();
            var bidDao = bidStr == null ? null : getDaoFromString(bidStr, BidDao.class);
            if (bidDao == null)
                continue;
            bids.put(bidId, bidDao);
        }

        return bids;
    }

    /**
     * Set the bid DAO in the cache.
     * 
     * @param jedis      The Redis client.
     * @param AuctionDao The bid DAO.
     */
    public static void setBid(Jedis jedis, BidDao AuctionDao) {
        var key = PREFIX_BID + AuctionDao.getId();
        setDao(jedis, key, AuctionDao);
    }

    /**
     * Delete the bid DAO from the cache.
     * 
     * @param jedis The Redis client.
     * @param bidId The bid id.
     */
    public static void removeBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        removeDao(jedis, key);
    }

    /* ------------------------- Question DAO ------------------------- */

    /**
     * Get the question DAO from the cache.
     * 
     * @param jedis      The Redis client.
     * @param questionId The question id.
     * @return The question DAO or null if not found.
     */
    public static QuestionDao getQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        return getDao(jedis, key, QuestionDao.class);
    }

    /**
     * Set the question DAO in the cache.
     * 
     * @param jedis      The Redis client.
     * @param AuctionDao The question DAO.
     */
    public static void setQuestion(Jedis jedis, QuestionDao questionDao) {
        var key = PREFIX_QUESTION + questionDao.getId();
        setDao(jedis, key, QuestionDao.class);
    }

    /**
     * Delete the question DAO from the cache.
     * 
     * @param jedis      The Redis client.
     * @param questionId The question id.
     */
    public static void removeQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        removeDao(jedis, key);
    }

    /* ------------------------- User DAO ------------------------- */

    /**
     * Get the user DAO from the cache.
     * 
     * @param jedis  The Redis client.
     * @param userId The user id.
     * @return The user DAO or null if not found.
     */
    public static UserDao getUser(Jedis jedis, String userId) {
        var key = PREFIX_USER + userId;
        return getDao(jedis, key, UserDao.class);
    }

    public static HashMap<String, UserDao> getUserMany(Jedis jedis, List<String> userIds) {
        var userResponses = new HashMap<String, Response<String>>(userIds.size());
        try (var pipeline = jedis.pipelined()) {
            for (var userId : userIds) {
                var userKey = PREFIX_USER + userId;
                userResponses.put(userId, pipeline.get(userKey));
            }
        }

        var users = new HashMap<String, UserDao>(userIds.size());
        for (var entry : userResponses.entrySet()) {
            var userId = entry.getKey();
            var userStr = entry.getValue().get();
            var userDao = userStr == null ? null : getDaoFromString(userStr, UserDao.class);
            if (userDao == null)
                continue;
            users.put(userId, userDao);
        }

        return users;
    }

    /**
     * Set the user DAO in the cache.
     * 
     * @param jedis The Redis client.
     * @param user  The user DAO.
     */
    public static void setUser(Jedis jedis, UserDao userDao) {
        var key = PREFIX_USER + userDao.getUserId();
        setDao(jedis, key, userDao);
    }

    /**
     * Delete the user DAO from the cache.
     * 
     * @param jedis  The Redis client.
     * @param userId The user id.
     */
    public static void removeUser(Jedis jedis, String userId) {
        var key = PREFIX_USER + userId;
        removeDao(jedis, key);
    }

    /* ------------------------- Session ------------------------- */

    public static void setSession(Jedis jedis, String userId, String token) {
        var key = PREFIX_USER_TOKEN + token;
        var value = userId;

        jedis.setex(key, TTL_SESSION, value);
    }

    public static String getSession(Jedis jedis, String token) {
        var key = PREFIX_USER_TOKEN + token;
        var value = jedis.get(key);
        if (value == null)
            return null;

        return value;
    }

    public static void removeSession(Jedis jedis, String token) {
        var key = PREFIX_USER_TOKEN + token;

        jedis.del(key);
    }

    /* ------------------------- Top Bid ------------------------- */

    public static void setTopBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_TOP_BID + auctionId;
        var value = bidId.toHexString();
        jedis.setex(key, TTL_DAO, value);
    }

    public static String getTopBid(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_TOP_BID + auctionId;
        return jedis.get(key);
    }

    /* ------------------------- Auction Bids ------------------------- */

    /**
     * Add a list of bids to the auction bids.
     * The amount of bids in cache is limited and any new bids will replace the
     * oldest ones.
     * 
     * @param jedis     The Redis client.
     * @param auctionId The auction id.
     * @param bidIds    The list of bid ids.
     */
    public static void pushAuctionBids(Jedis jedis, ObjectId auctionId, List<ObjectId> bidIds) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        var bidIdsStr = (String[]) bidIds.stream().map(ObjectId::toHexString).toArray();
        jedis.lpush(key, bidIdsStr);
        jedis.ltrim(key, 0, MAX_AUCTION_BIDS_LIST_SIZE);
    }

    /**
     * Request a list of bids from the auction bids.
     * If the requested bids are not all in cache, null is returned and they should
     * be fetched from the database.
     * 
     * @param jedis     The Redis client.
     * @param auctionId The auction id.
     * @param skip      The number of bids to skip.
     * @param limit     The number of bids to return.
     * @return The list of bid ids or null if not all bids are in cache.
     */
    public static List<ObjectId> getAuctionBids(Jedis jedis, ObjectId auctionId, int skip, int limit) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        var bidIdsStr = jedis.lrange(key, skip, skip + limit - 1);
        if (bidIdsStr.size() != limit)
            return null;
        return bidIdsStr.stream().map(ObjectId::new).toList();
    }

    /* ------------------------- Mixed ------------------------- */

    public static void addAuctionBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        addDaoId(jedis, key, bidId);
    }

    public static void setAuctionQuestions(Jedis jedis, ObjectId auctionId, List<ObjectId> questions) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        setDaoIdList(jedis, key, questions.stream().map(ObjectId::toHexString).toList());
    }

    public static List<ObjectId> getAuctionQuestions(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        return getDaoIdList(jedis, key).stream().map(ObjectId::new).toList();
    }

    public static void addAuctionQuestion(Jedis jedis, ObjectId auctionId, String questionId) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        addDaoId(jedis, key, questionId);
    }

    public static void setUserAuctions(Jedis jedis, String userId, List<String> auctions) {
        var key = PREFIX_USER_AUCTIONS + userId;
        setDaoIdList(jedis, key, auctions);
    }

    public static List<ObjectId> getUserAuctions(Jedis jedis, String userId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        return getDaoIdList(jedis, key).stream().map(ObjectId::new).toList();
    }

    public static void addUserAuction(Jedis jedis, String userId, ObjectId auctionId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        addDaoId(jedis, key, auctionId);
    }

    public static void setUserFollowedAuctions(Jedis jedis, String userId, List<String> auctions) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        setDaoIdList(jedis, key, auctions);
    }

    public static List<ObjectId> getUserFollowedAuctions(Jedis jedis, String userId) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        return getDaoIdList(jedis, key).stream().map(ObjectId::new).toList();
    }

    public static void addUserFollowedAuction(Jedis jedis, String userId, ObjectId auctionId) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        addDaoId(jedis, key, auctionId);
    }

    /* ----------------------- Soon To Close Tracking ----------------------- */

    public record AuctionSoonToClose(String auctionId, LocalDateTime time) {
    }

    public static void setAuctionsSoonToClose(Jedis jedis, List<AuctionSoonToClose> auctions) {
        var key = KEY_AUCTIONS_ABOUNT_TO_CLOSE;
        try (var multi = jedis.multi()) {
            multi.del(key);
            for (var auction : auctions)
                multi.zadd(key, auction.time.toEpochSecond(ZoneOffset.UTC), auction.auctionId);
            multi.exec();
        }
    }

    public static void addAuctionSoonToClose(Jedis jedis, AuctionSoonToClose auction) {
        var key = KEY_AUCTIONS_ABOUNT_TO_CLOSE;
        jedis.zadd(key, auction.time.toEpochSecond(ZoneOffset.UTC), auction.auctionId);
    }

    public static void removeAuctionSoonToClose(Jedis jedis, String auctionId) {
        var key = KEY_AUCTIONS_ABOUNT_TO_CLOSE;
        jedis.zrem(key, auctionId);
    }

    public static List<ObjectId> getAuctionsSoonToClose(Jedis jedis) {
        var key = KEY_AUCTIONS_ABOUNT_TO_CLOSE;
        return jedis.zrevrange(key, 0, AppLogic.MAX_ABOUT_TO_CLOSE_AUCTIONS - 1)
                .stream().map(ObjectId::new).toList();
    }

    /* ------------------------- Recent Tracking ------------------------- */

    public static void pushRecentAuction(Jedis jedis, String auctionId) {
        var key = KEY_RECENT_AUCTIONS;
        jedis.lpush(key, auctionId);
        jedis.ltrim(key, 0, AppLogic.MAX_RECENT_AUCTIONS - 1);
    }

    public static List<String> getRecentAuctions(Jedis jedis) {
        var key = KEY_RECENT_AUCTIONS;
        return jedis.lrange(key, 0, -1);
    }

    /* ------------------------- Popularity Tracking ------------------------- */

    public static List<ObjectId> getPopularAuctions(Jedis jedis) {
        var key = KEY_POPULAR_AUCTIONS;
        return jedis.lrange(key, 0, -1).stream().map(ObjectId::new).toList();
    }

    public static void updatePopularAuctions(Jedis jedis) {
        var mostPopular = jedis.zrevrange(KEY_POPULAR_AUCTIONS_RANKING, 0, AppLogic.MAX_MOST_POPULAR_AUCTIONS);
        jedis.del(KEY_POPULAR_AUCTIONS_RANKING);
        if (mostPopular.size() > 0) {
            jedis.lpush(KEY_POPULAR_AUCTIONS, mostPopular.toArray(new String[mostPopular.size()]));
            jedis.ltrim(KEY_POPULAR_AUCTIONS, 0, AppLogic.MAX_MOST_POPULAR_AUCTIONS - 1);
        }
    }

    public static void incrementPopularAuction(Jedis jedis, String auctionId) {
        jedis.zincrby(KEY_POPULAR_AUCTIONS_RANKING, 1, auctionId);
    }

    /* ------------------------- Internal Helpers ------------------------- */

    private static <T> T getDao(Jedis jedis, String key, Class<T> clazz) {
        try {
            var json = jedis.get(key);
            if (json == null)
                return null;
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static <T> T getDaoFromString(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void setDao(Jedis jedis, String key, Object dao) {
        try {
            var json = mapper.writeValueAsString(dao);
            jedis.set(key, json);
            jedis.expire(key, TTL_DAO);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void removeDao(Jedis jedis, String key) {
        jedis.del(key);
    }

    private static void setDaoIdList(Jedis jedis, String key, List<String> ids) {
        for (var id : ids)
            jedis.rpush(key, id);
        jedis.expire(key, TTL_DAO);
    }

    private static void addDaoId(Jedis jedis, String key, ObjectId id) {
        addDaoId(jedis, key, id.toHexString());
    }

    private static void addDaoId(Jedis jedis, String key, String id) {
        if (jedis.exists(key))
            jedis.rpush(key, id);
        jedis.expire(key, TTL_DAO);
    }

    private static List<String> getDaoIdList(Jedis jedis, String key) {
        var l = jedis.lrange(key, 0, -1);
        if (l.isEmpty())
            return null;
        return l;
    }
}
