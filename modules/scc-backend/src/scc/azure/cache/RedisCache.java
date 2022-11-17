package scc.azure.cache;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;

import java.util.List;

public class RedisCache implements Cache {
    private static final String USER_AUCTIONS_PREFIX = "user_auctions:";
    private static final String AUCTION_BIDS_PREFIX = "auction_bids:";
    private static final String AUCTION_QUESTIONS_PREFIX = "auction_questions:";
    private static final String MEDIA_PREFIX = "media:";

    private final JedisPool jedisPool;
    private final Gson gson;

    public RedisCache(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.gson = new Gson();
    }

    @Override
    public void addUserAuction(AuctionDAO auctionDAO) {
        var client = getClientInstance();
        var key = createUserAuctionListKey(auctionDAO.getUserId());
        var value = this.gson.toJson(auctionDAO);

        client.sadd(key, value);
    }

    @Override
    public void removeUserAuction(AuctionDAO auctionDAO) {
        var client = getClientInstance();
        var key = createUserAuctionListKey(auctionDAO.getUserId());
        var value = this.gson.toJson(auctionDAO);

        client.srem(key, value);
    }

    @Override
    public List<AuctionDAO> getUserAuctions(String userId) {
        var client = getClientInstance();
        var key = createUserAuctionListKey(userId);

        var value = client.smembers(key);
        if (value == null)
            return null;
        return value.stream().map(s -> gson.fromJson(s, AuctionDAO.class)).toList();
    }

    @Override
    public void deleteUser(String userId) {
        var client = getClientInstance();
        var key = createUserAuctionListKey(userId);

        client.del(key);
    }

    @Override
    public void addAuctionBid(BidDAO bidDAO) {
        var client = getClientInstance();
        var key = createAuctionBidListKey(bidDAO.getAuctionId());
        var value = this.gson.toJson(bidDAO);

        client.zadd(key, bidDAO.getAmount(), value);
    }

    @Override
    public void removeAuctionBid(BidDAO bidDAO) {
        var client = getClientInstance();
        var key = createAuctionBidListKey(bidDAO.getAuctionId());
        var value = this.gson.toJson(bidDAO);

        client.zrem(key, value);
    }

    @Override
    public List<BidDAO> getAuctionBids(String auctionId) {
        var client = getClientInstance();
        var key = createAuctionBidListKey(auctionId);

        var value = client.zrange(key, 0, -1);
        if (value == null)
            return null;
        return value.stream().map(s -> gson.fromJson(s, BidDAO.class)).toList();
    }


    @Override
    public void addAuctionQuestion(QuestionDAO questionDAO) {
        var client = getClientInstance();
        var key = createAuctionQuestionListKey(questionDAO.getAuctionId());
        var value = this.gson.toJson(questionDAO);

        client.sadd(key, value);
    }

    @Override
    public void removeAuctionQuestion(QuestionDAO questionDAO) {
        var client = getClientInstance();
        var key = createAuctionQuestionListKey(questionDAO.getAuctionId());
        var value = this.gson.toJson(questionDAO);

        client.srem(key, value);
    }

    @Override
    public List<QuestionDAO> getAuctionQuestions(String auctionId) {
        var client = getClientInstance();
        var key = createAuctionQuestionListKey(auctionId);

        var value = client.smembers(key);
        if (value == null)
            return null;
        return value.stream().map(s -> gson.fromJson(s, QuestionDAO.class)).toList();
    }

    @Override
    public void deleteAuction(String auctionId) {
        var client = getClientInstance();

        var key1 = createAuctionBidListKey(auctionId);
        var key2 = createAuctionQuestionListKey(auctionId);
        client.del(key1, key2);

        //TODO need to also delete auction from list of user auctions
    }

    @Override
    public void updateAuction(AuctionDAO oldValue, AuctionDAO newValue) {
        removeUserAuction(oldValue);
        addUserAuction(newValue);
    }

    @Override
    public void updateQuestion(QuestionDAO oldValue, QuestionDAO newValue) {
        removeAuctionQuestion(oldValue);
        addAuctionQuestion(newValue);
    }

    @Override
    public List<AuctionDAO> getAboutToCloseAuctions() {
        return null;
    }

    @Override
    public void addAboutToCloseAuctions(List<AuctionDAO> auctions) {

    }

    @Override
    public List<AuctionDAO> getRecentAuctions() {
        return null;
    }

    @Override
    public void addRecentAuction(AuctionDAO auctionDAO) {

    }

    @Override
    public List<AuctionDAO> getPopularAuctions() {
        return null;
    }

    @Override
    public void addPopularAuctions(AuctionDAO auctionDAO) {

    }

    @Override
    public void setMedia(String mediaId, byte[] contents) {
        var client = getClientInstance();
        var key = createMediaKey(mediaId);

        // TODO maybe need to use base64 for this to work
        client.set(key.getBytes(), contents);
    }

    @Override
    public void deleteMedia(String mediaId) {
        var client = getClientInstance();
        var key = createMediaKey(mediaId);

        client.del(key.getBytes());
    }

    @Override
    public byte[] getMedia(String mediaId) {
        var client = getClientInstance();
        var key = createMediaKey(mediaId);

        return client.get(key.getBytes());
    }


    private String createUserAuctionListKey(String userId) {
        return USER_AUCTIONS_PREFIX + userId;
    }

    private String createAuctionBidListKey(String auctionId) {
        return AUCTION_BIDS_PREFIX + auctionId;
    }

    private String createAuctionQuestionListKey(String auctionId) {
        return AUCTION_QUESTIONS_PREFIX + auctionId;
    }

    private String createMediaKey(String mediaId) {
        return MEDIA_PREFIX + mediaId;
    }

    private Jedis getClientInstance() {
        return jedisPool.getResource();
    }
}
