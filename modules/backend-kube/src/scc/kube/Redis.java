package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
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

    private static final ObjectMapper mapper = Kube.createObjectMapper();

    /* ------------------------- Auction DAO ------------------------- */

    public static AuctionDao getAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        return getDao(jedis, key, AuctionDao.class);
    }

    public static void setAuction(Jedis jedis, AuctionDao AuctionDao) {
        var key = PREFIX_AUCTION + AuctionDao.getId();
        setDao(jedis, key, AuctionDao);
    }

    public static void removeAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        removeDao(jedis, key);
    }

    /* ------------------------- Bid DAO ------------------------- */

    public static BidDao getBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        return getDao(jedis, key, BidDao.class);
    }

    public static void setBid(Jedis jedis, BidDao AuctionDao) {
        var key = PREFIX_BID + AuctionDao.getId();
        setDao(jedis, key, AuctionDao);
    }

    public static void removeBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        removeDao(jedis, key);
    }

    /* ------------------------- Question DAO ------------------------- */

    public static QuestionDao getQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        return getDao(jedis, key, QuestionDao.class);
    }

    public static void setQuestion(Jedis jedis, QuestionDao questionDao) {
        var key = PREFIX_QUESTION + questionDao.getId();
        setDao(jedis, key, QuestionDao.class);
    }

    public static void removeQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        removeDao(jedis, key);
    }

    /* ------------------------- User DAO ------------------------- */

    public static UserDao getUser(Jedis jedis, String userId) {
        var key = PREFIX_USER + userId;
        return getDao(jedis, key, UserDao.class);
    }

    public static void setUser(Jedis jedis, UserDao userDao) {
        var key = PREFIX_USER + userDao.getUserId();
        setDao(jedis, key, userDao);
    }

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

    /* ------------------------- Mixed ------------------------- */

    public static void setTopBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_TOP_BID + auctionId;
        var value = bidId.toHexString();
        jedis.setex(key, TTL_DAO, value);
    }

    public static String getTopBid(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_TOP_BID + auctionId;
        return jedis.get(key);
    }

    public static void setAuctionBids(Jedis jedis, ObjectId auctionId, List<String> bids) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        setDaoIdList(jedis, key, bids);
    }

    public static List<String> getAuctionBids(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        return getDaoIdList(jedis, key);
    }

    public static void addAuctionBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        addDaoId(jedis, key, bidId);
    }

    public static void setAuctionQuestions(Jedis jedis, ObjectId auctionId, List<ObjectId> questions) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        setDaoIdList(jedis, key, questions.stream().map(ObjectId::toHexString).toList());
    }

    public static List<String> getAuctionQuestions(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        return getDaoIdList(jedis, key);
    }

    public static void addAuctionQuestion(Jedis jedis, ObjectId auctionId, String questionId) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        addDaoId(jedis, key, questionId);
    }

    public static void setUserAuctions(Jedis jedis, String userId, List<String> auctions) {
        var key = PREFIX_USER_AUCTIONS + userId;
        setDaoIdList(jedis, key, auctions);
    }

    public static List<String> getUserAuctions(Jedis jedis, String userId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        return getDaoIdList(jedis, key);
    }

    public static void addUserAuction(Jedis jedis, String userId, ObjectId auctionId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        addDaoId(jedis, key, auctionId);
    }

    public static void setUserFollowedAuctions(Jedis jedis, String userId, List<String> auctions) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        setDaoIdList(jedis, key, auctions);
    }

    public static List<String> getUserFollowedAuctions(Jedis jedis, String userId) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        return getDaoIdList(jedis, key);
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

    public static List<String> getAuctionsSoonToClose(Jedis jedis) {
        var key = KEY_AUCTIONS_ABOUNT_TO_CLOSE;
        return jedis.zrevrange(key, 0, AppLogic.MAX_ABOUT_TO_CLOSE_AUCTIONS - 1);
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

    public static List<String> getPopularAuctions(Jedis jedis) {
        var key = KEY_POPULAR_AUCTIONS;
        return jedis.lrange(key, 0, -1);
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
