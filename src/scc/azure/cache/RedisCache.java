package scc.azure.cache;

import java.util.List;

import com.google.gson.Gson;

import redis.clients.jedis.JedisPool;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;

public class RedisCache implements Cache {
    private static final String USER_PREFIX = "user:";
    private static final String AUCTION_PREFIFX = "auction:";
    private static final String BID_PREFIX = "bid:";
    private static final String QUESTION_PREFIX = "question:";
    private static final String AUCTION_QUEST_LIST_PREIX = "auction:questions:";

    private final JedisPool jedisPool;
    private final Gson gson;

    public RedisCache(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.gson = new Gson();
    }

    @Override
    public void setUser(UserDAO user) {
        var key = this.createUserKey(USER_PREFIX);
        var value = this.gson.toJson(user);
        try (var client = this.jedisPool.getResource()) {
            client.set(key, value);
        }
    }

    @Override
    public void unsetUser(String userId) {
        var key = this.createUserKey(userId);
        try (var client = this.jedisPool.getResource()) {
            client.del(key);
        }
    }

    @Override
    public UserDAO getUser(String userId) {
        var key = this.createUserKey(userId);
        try (var client = this.jedisPool.getResource()) {
            var value = client.get(key);
            if (value == null)
                return null;
            return this.gson.fromJson(value, UserDAO.class);
        }
    }

    @Override
    public void setUserAuction(String userId, String auctionId) {

    }

    @Override
    public List<AuctionDAO> getUserAuctions(String userId) {
        return null;
    }

    @Override
    public void setAuction(AuctionDAO auction) {
        var key = this.createAuctionKey(auction.getId());
        var value = this.gson.toJson(auction);
        try (var client = this.jedisPool.getResource()) {
            client.set(key, value);
        }
    }

    @Override
    public void unsetAuction(String auctionId) {
        var key = this.createAuctionKey(auctionId);
        try (var client = this.jedisPool.getResource()) {
            client.del(key);
        }
    }

    @Override
    public AuctionDAO getAuction(String auctionId) {
        var key = this.createAuctionKey(auctionId);
        try (var client = this.jedisPool.getResource()) {
            var value = client.get(key);
            if (value == null)
                return null;
            return this.gson.fromJson(value, AuctionDAO.class);
        }
    }

    @Override
    public void setAuctionBid(String bidId) {

    }

    @Override
    public List<BidDAO> getAuctionBids(String auctionId) {
        return null;
    }

    @Override
    public void setBid(BidDAO bid) {
        var key = this.createBidKey(bid.getId());
        var value = this.gson.toJson(bid);
        try (var client = this.jedisPool.getResource()) {
            client.set(key, value);
        }
    }

    @Override
    public void unsetBid(String bidId) {
        var key = this.createBidKey(bidId);
        try (var client = this.jedisPool.getResource()) {
            client.del(key);
        }
    }

    @Override
    public BidDAO getBid(String bidId) {
        var key = this.createBidKey(bidId);
        try (var client = this.jedisPool.getResource()) {
            var value = client.get(key);
            if (value == null)
                return null;
            return this.gson.fromJson(value, BidDAO.class);
        }
    }

    @Override
    public void setQuestion(QuestionDAO question) {
        var key = this.createQuestionKey(question.getId());
        var value = this.gson.toJson(question);
        try (var client = this.jedisPool.getResource()) {
            client.set(key, value);
        }
    }

    @Override
    public void unsetQuestion(String questionId) {
        var key = this.createQuestionKey(questionId);
        try (var client = this.jedisPool.getResource()) {
            client.del(key);
        }
    }

    @Override
    public QuestionDAO getQuestion(String questionId) {
        var key = this.createQuestionKey(questionId);
        try (var client = this.jedisPool.getResource()) {
            var value = client.get(key);
            if (value == null)
                return null;
            return this.gson.fromJson(value, QuestionDAO.class);
        }
    }

    @Override
    public void addAuctionQuestion(String auctionId, String questionId) {
        var key = this.createAuctionQuestionListKey(auctionId);
        try (var client = this.jedisPool.getResource()) {
            client.sadd(key, questionId);
        }
    }

    @Override
    public void removeAuctionQuestion(String auctionId, String questionId) {
        var key = this.createAuctionQuestionListKey(auctionId);
        try (var client = this.jedisPool.getResource()) {
            client.srem(key, questionId);
        }
    }

    @Override
    public List<String> getAuctionQuestions(String auctionId) {
        var key = this.createAuctionQuestionListKey(auctionId);
        try (var client = this.jedisPool.getResource()) {
            return client.smembers(key).stream().toList();
        }
    }

    private String createUserKey(String userId) {
        return USER_PREFIX + userId;
    }

    private String createAuctionKey(String auctionId) {
        return AUCTION_PREFIFX + auctionId;
    }

    private String createBidKey(String bidId) {
        return BID_PREFIX + bidId;
    }

    private String createQuestionKey(String questionId) {
        return QUESTION_PREFIX + questionId;
    }

    private String createAuctionQuestionListKey(String auctionId) {
        return AUCTION_QUEST_LIST_PREIX + auctionId;
    }

}
