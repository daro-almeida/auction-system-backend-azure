package scc.azure;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import redis.clients.jedis.Jedis;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;
import scc.azure.utils.LocalDateTimeConverter;
import scc.azure.utils.ZonedDateTimeConverter;

public class Redis {
    public static final int TTL_DAO = 60 * 60;
    public static final int TTL_SESSION = 30 * 60;

    public static final int MAX_RECENT_AUCTIONS = 20;

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

    private static final Gson gson = createGson();

    /* ------------------------- Auction DAO ------------------------- */

    public static AuctionDAO getAuction(Jedis jedis, String auctionId) {
        var key = PREFIX_AUCTION + auctionId;
        var value = jedis.get(key);
        if (value == null)
            return null;

        return gson.fromJson(value, AuctionDAO.class);
    }

    public static void setAuction(Jedis jedis, AuctionDAO auctionDAO) {
        var key = PREFIX_AUCTION + auctionDAO.getId();
        var value = gson.toJson(auctionDAO);

        jedis.setex(key, TTL_DAO, value);
    }

    public static void removeAuction(Jedis jedis, String auctionId) {
        var key = PREFIX_AUCTION + auctionId;

        jedis.del(key);
    }

    /* ------------------------- Bid DAO ------------------------- */

    public static BidDAO getBid(Jedis jedis, String bidId) {
        var key = PREFIX_BID + bidId;
        var value = jedis.get(key);
        if (value == null)
            return null;

        return gson.fromJson(value, BidDAO.class);
    }

    public static void setBid(Jedis jedis, BidDAO auctionDAO) {
        var key = PREFIX_BID + auctionDAO.getId();
        var value = gson.toJson(auctionDAO);

        jedis.setex(key, TTL_DAO, value);
    }

    public static void removeBid(Jedis jedis, String bidId) {
        var key = PREFIX_BID + bidId;

        jedis.del(key);
    }

    /* ------------------------- Question DAO ------------------------- */

    public static QuestionDAO getQuestion(Jedis jedis, String questionId) {
        var key = PREFIX_QUESTION + questionId;
        var value = jedis.get(key);
        if (value == null)
            return null;

        return gson.fromJson(value, QuestionDAO.class);
    }

    public static void setQuestion(Jedis jedis, QuestionDAO auctionDAO) {
        var key = PREFIX_QUESTION + auctionDAO.getId();
        var value = gson.toJson(auctionDAO);

        jedis.setex(key, TTL_DAO, value);
    }

    public static void removeQuestion(Jedis jedis, String questionId) {
        var key = PREFIX_QUESTION + questionId;

        jedis.del(key);
    }

    /* ------------------------- User DAO ------------------------- */

    public static UserDAO getUser(Jedis jedis, String userId) {
        var key = PREFIX_USER + userId;
        var value = jedis.get(key);
        if (value == null)
            return null;

        return gson.fromJson(value, UserDAO.class);
    }

    public static void setUser(Jedis jedis, UserDAO auctionDAO) {
        var key = PREFIX_USER + auctionDAO.getId();
        var value = gson.toJson(auctionDAO);

        jedis.setex(key, TTL_DAO, value);
    }

    public static void removeUser(Jedis jedis, String userId) {
        var key = PREFIX_USER + userId;

        jedis.del(key);
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

    public static void setTopBid(Jedis jedis, String auctionId, String bidId) {
        var key = PREFIX_TOP_BID + auctionId;
        var value = bidId;

        jedis.setex(key, TTL_DAO, value);
    }

    public static String getTopBid(Jedis jedis, String auctionId) {
        var key = PREFIX_TOP_BID + auctionId;
        return jedis.get(key);
    }

    public static void setAuctionBids(Jedis jedis, String auctionId, List<String> bids) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        for (var bid : bids)
            jedis.rpush(key, bid);
        jedis.expire(key, TTL_DAO);
    }

    public static List<String> getAuctionBids(Jedis jedis, String auctionId) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        return jedis.lrange(key, 0, -1);
    }

    public static void addAuctionBid(Jedis jedis, String auctionId, String bidId) {
        var key = PREFIX_AUCTION_BIDS + auctionId;
        if (!jedis.exists(key))
            return;
        jedis.rpush(key, bidId);
        jedis.expire(key, TTL_DAO);
    }

    public static void setAuctionQuestions(Jedis jedis, String auctionId, List<String> questions) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        for (var question : questions)
            jedis.rpush(key, question);
        jedis.expire(key, TTL_DAO);
    }

    public static List<String> getAuctionQuestions(Jedis jedis, String auctionId) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        return jedis.lrange(key, 0, -1);
    }

    public static void addAuctionQuestion(Jedis jedis, String auctionId, String questionId) {
        var key = PREFIX_AUCTION_QUESTIONS + auctionId;
        if (!jedis.exists(key))
            return;
        jedis.rpush(key, questionId);
        jedis.expire(key, TTL_DAO);
    }

    public static void setUserAuctions(Jedis jedis, String userId, List<String> auctions) {
        var key = PREFIX_USER_AUCTIONS + userId;
        for (var auction : auctions)
            jedis.rpush(key, auction);
        jedis.expire(key, TTL_DAO);
    }

    public static List<String> getUserAuctions(Jedis jedis, String userId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        return jedis.lrange(key, 0, -1);
    }

    public static void addUserAuction(Jedis jedis, String userId, String auctionId) {
        var key = PREFIX_USER_AUCTIONS + userId;
        if (!jedis.exists(key))
            return;
        jedis.rpush(key, auctionId);
        jedis.expire(key, TTL_DAO);
    }

    public static void setUserFollowedAuctions(Jedis jedis, String userId, List<String> auctions) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        for (var auction : auctions)
            jedis.rpush(key, auction);
        jedis.expire(key, TTL_DAO);
    }

    public static List<String> getUserFollowedAuctions(Jedis jedis, String userId) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        return jedis.lrange(key, 0, -1);
    }

    public static void addUserFollowedAuction(Jedis jedis, String userId, String auctionId) {
        var key = PREFIX_USER_FOLLOWED_AUCTIONS + userId;
        if (!jedis.exists(key))
            return;
        jedis.rpush(key, auctionId);
        jedis.expire(key, TTL_DAO);
    }

    public static void setAuctionsAboutToClose(Jedis jedis, List<String> auctions) {
        var key = KEY_AUCTIONS_ABOUNT_TO_CLOSE;
        for (var auction : auctions)
            jedis.rpush(key, auction);
    }

    public static List<String> getAuctionsAboutToClose(Jedis jedis) {
        var key = KEY_AUCTIONS_ABOUNT_TO_CLOSE;
        return jedis.lrange(key, 0, -1);
    }

    public static void pushRecentAuction(Jedis jedis, String auctionId) {
        var key = KEY_RECENT_AUCTIONS;
        jedis.lpush(key, auctionId);
        jedis.ltrim(key, 0, MAX_RECENT_AUCTIONS - 1);
    }

    public static List<String> getRecentAuctions(Jedis jedis) {
        var key = KEY_RECENT_AUCTIONS;
        return jedis.lrange(key, 0, -1);
    }

    public static List<String> getPopularAuctions(Jedis jedis) {
        var key = KEY_POPULAR_AUCTIONS;
        return jedis.lrange(key, 0, -1);
    }

    /* ------------------------- Internal Helpers ------------------------- */
    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeConverter())
                .create();
    }
}
