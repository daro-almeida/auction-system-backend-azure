package scc.kube;

import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import scc.PagingWindow;
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

    public static final String KEY_AUCTIONS_ABOUNT_TO_CLOSE = "auctions-about-to-close";
    public static final String KEY_RECENT_AUCTIONS = "recent-auctions";
    public static final String KEY_POPULAR_AUCTIONS = "popular-auctions";
    public static final String KEY_POPULAR_AUCTIONS_RANKING = "popular-auctions-ranking";

    private static final int MAX_AUCTION_BIDS = 256;

    private static final ObjectMapper mapper = Kube.createObjectMapper();

    /* ------------------------- Auction ------------------------- */

    public static void setAuction(Jedis jedis, AuctionDao auctionDao) {
        var key = PREFIX_AUCTION + auctionDao.id;
        setDao(jedis, key, auctionDao);
    }

    public static void setAuctionMany(Jedis jedis, List<AuctionDao> auctionDaos) {
        try (var pipeline = jedis.pipelined()) {
            for (var auctionDao : auctionDaos) {
                var key = PREFIX_AUCTION + auctionDao.id;
                setDao(pipeline, key, auctionDao);
            }
        }
    }

    public static void unsetAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        unsetDao(jedis, key);
    }

    public static AuctionDao getAuction(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        return getDao(jedis, key, AuctionDao.class);
    }

    public static HashMap<ObjectId, AuctionDao> getAuctionMany(Jedis jedis, List<ObjectId> auctionIds) {
        return getDaoMany(jedis, PREFIX_AUCTION, auctionIds, AuctionDao.class);
    }

    public static void pushAuctionBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        jedis.lpush(key, bidId.toString());
        jedis.ltrim(key, 0, MAX_AUCTION_BIDS - 1);
        jedis.expire(key, TTL_DAO);
    }

    public static List<ObjectId> getAuctionBids(Jedis jedis, ObjectId auctionId, int skip, int limit) {
        if (limit > MAX_AUCTION_BIDS)
            return null;
        var key = PREFIX_AUCTION_BIDS + auctionId;
        var bidIds = jedis.lrange(key, skip, skip + limit - 1).stream().map(ObjectId::new).toList();
        if (bidIds.size() < limit)
            return null;
        return bidIds;
    }

    public static void setAuctionTopBid(Jedis jedis, ObjectId auctionId, ObjectId bidId) {
        var key = PREFIX_TOP_BID + auctionId;
        jedis.set(key, bidId.toHexString());
    }

    public static ObjectId getAuctionTopBid(Jedis jedis, ObjectId auctionId) {
        var key = PREFIX_TOP_BID + auctionId;
        var bidId = jedis.get(key);
        if (bidId == null)
            return null;
        return new ObjectId(bidId);
    }

    /* ------------------------- Bid ------------------------- */

    public static void setBid(Jedis jedis, BidDao bidDao) {
        var key = PREFIX_BID + bidDao.id;
        setDao(jedis, key, bidDao);
    }

    public static void unsetBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        unsetDao(jedis, key);
    }

    public static BidDao getBid(Jedis jedis, ObjectId bidId) {
        var key = PREFIX_BID + bidId;
        return getDao(jedis, key, BidDao.class);
    }

    public static HashMap<ObjectId, BidDao> getBidMany(Jedis jedis, List<ObjectId> bidIds) {
        return getDaoMany(jedis, PREFIX_BID, bidIds, BidDao.class);
    }

    /* ------------------------- Question ------------------------- */

    public static void setQuestion(Jedis jedis, QuestionDao questionDao) {
        var key = PREFIX_QUESTION + questionDao.id;
        setDao(jedis, key, questionDao);
    }

    public static void unsetQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        unsetDao(jedis, key);
    }

    public static QuestionDao getQuestion(Jedis jedis, ObjectId questionId) {
        var key = PREFIX_QUESTION + questionId;
        return getDao(jedis, key, QuestionDao.class);
    }

    public static HashMap<ObjectId, QuestionDao> getQuestionMany(Jedis jedis, List<ObjectId> questionIds) {
        return getDaoMany(jedis, PREFIX_QUESTION, questionIds, QuestionDao.class);
    }

    /* ------------------------- User ------------------------- */

    public static void setUser(Jedis jedis, UserDao userDao) {
        var key = PREFIX_USER + userDao.id;
        setDao(jedis, key, userDao);
    }

    public static void unsetUser(Jedis jedis, ObjectId userId) {
        var key = PREFIX_USER + userId;
        unsetDao(jedis, key);
    }

    public static UserDao getUser(Jedis jedis, ObjectId userId) {
        var key = PREFIX_USER + userId;
        return getDao(jedis, key, UserDao.class);
    }

    public static void setUserAuctions(Jedis jedis, ObjectId userId, List<ObjectId> auctionIds) {
        var key = PREFIX_USER_AUCTIONS + userId;
        jedis.del(key);
        jedis.rpush(key, auctionIds.stream().map(ObjectId::toString).toArray(String[]::new));
        jedis.expire(key, TTL_DAO);
    }

    public static void pushUserAuction(Jedis jedis, ObjectId userId, ObjectId auctionId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        jedis.rpush(key, auctionId.toString());
        jedis.expire(key, TTL_DAO);
    }

    public static List<ObjectId> getUserAuctions(Jedis jedis, ObjectId userId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        var auctionIds = jedis.lrange(key, 0, -1).stream().map(ObjectId::new).toList();
        return auctionIds;
    }

    public static void addUserFollowedAuction(Jedis jedis, ObjectId userId, ObjectId auctionId) {
        addUserFollowedAuctions(jedis, userId, List.of(auctionId));
    }

    public static void addUserFollowedAuctions(Jedis jedis, ObjectId userId, List<ObjectId> auctionIds) {
        try (var pipeline = jedis.pipelined()) {
            for (var auctionId : auctionIds) {
                var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
                jedis.sadd(key, auctionId.toString());
                jedis.expire(key, TTL_DAO);
            }
        }
    }

    public static List<ObjectId> getUserFollowedAuctions(Jedis jedis, ObjectId userId) {
        // TODO: use ordered set here
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        var auctionIds = jedis.smembers(key).stream().map(ObjectId::new).toList();
        return auctionIds;
    }

    /* ------------------------- Session ------------------------- */

    public static void setSession(Jedis jedis, ObjectId userId, String token) {
        var key = PREFIX_USER_TOKEN + token;
        var value = userId.toHexString();

        jedis.setex(key, TTL_SESSION, value);
    }

    public static ObjectId getSession(Jedis jedis, String token) {
        var key = PREFIX_USER_TOKEN + token;
        var value = jedis.get(key);
        if (value == null)
            return null;

        return new ObjectId(value);
    }

    public static void removeSession(Jedis jedis, String token) {
        var key = PREFIX_USER_TOKEN + token;
        jedis.del(key);
    }

    /* ------------------------- Internal ------------------------- */

    private static <T> String daoToString(T dao) {
        try {
            return mapper.writeValueAsString(dao);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static <T> T stringToDao(String string, Class<T> clazz) {
        try {
            return mapper.readValue(string, clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static <T> void setDao(Jedis jedis, String key, T dao) {
        jedis.set(key, daoToString(dao));
        jedis.expire(key, TTL_DAO);
    }

    private static <T> void setDao(Pipeline jedis, String key, T dao) {
        jedis.set(key, daoToString(dao));
        jedis.expire(key, TTL_DAO);
    }

    private static void unsetDao(Jedis jedis, String key) {
        jedis.del(key);
    }

    private static <T> T getDao(Jedis jedis, String key, Class<T> clazz) {
        try {
            var json = jedis.get(key);
            if (json == null)
                return null;
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static <T> HashMap<ObjectId, T> getDaoMany(Jedis jedis, String prefix, List<ObjectId> ids, Class<T> clazz) {
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
            var dao = stringToDao(response.get(), clazz);
            daos.put(bidId, dao);
        }

        return daos;
    }
}