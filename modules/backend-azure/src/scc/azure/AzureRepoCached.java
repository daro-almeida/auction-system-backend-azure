package scc.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.QuestionDAO.Reply;
import scc.azure.dao.UserDAO;
import scc.azure.repo.AuctionRepo;
import scc.azure.repo.BidRepo;
import scc.azure.repo.QuestionRepo;
import scc.azure.repo.UserRepo;

public class AzureRepoCached implements AuctionRepo, BidRepo, QuestionRepo, UserRepo {
    private static final Logger logger = Logger.getLogger(AzureRepoCached.class.getName());

    private final AzureRepo repo;
    private final JedisPool jedisPool;

    public AzureRepoCached(AzureRepo repo, JedisPool jedisPool) {
        this.repo = repo;
        this.jedisPool = jedisPool;

    }

    @Override
    public Result<UserDAO, ServiceError> getUser(String id) {
        try (var jedis = jedisPool.getResource()) {
            var user = Redis.getUser(jedis, id);
            if (user != null)
                return Result.ok(user);
            var result = repo.getUser(id);
            if (result.isOk())
                Redis.setUser(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<UserDAO, ServiceError> insertUser(UserDAO user) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.insertUser(user);
            if (result.isOk())
                Redis.setUser(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<UserDAO, ServiceError> updateUser(UserDAO user) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.updateUser(user);
            if (result.isOk())
                Redis.setUser(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<UserDAO, ServiceError> deleteUser(String id) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.deleteUser(id);
            if (result.isOk())
                Redis.removeUser(jedis, id);
            return result;
        }
    }

    @Override
    public Result<QuestionDAO, ServiceError> getQuestion(String id) {
        try (var jedis = jedisPool.getResource()) {
            var question = Redis.getQuestion(jedis, id);
            if (question != null)
                return Result.ok(question);
            var result = repo.getQuestion(id);
            if (result.isOk())
                Redis.setQuestion(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<QuestionDAO, ServiceError> insertQuestion(QuestionDAO question) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.insertQuestion(question);
            if (result.isOk())
                Redis.setQuestion(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<QuestionDAO, ServiceError> insertReply(String id, Reply reply) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.insertReply(id, reply);
            if (result.isOk())
                Redis.setQuestion(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<List<QuestionDAO>, ServiceError> listAuctionQuestions(String auctionId) {
        try (var jedis = jedisPool.getResource()) {
            var questions = Redis.getAuctionQuestions(jedis, auctionId);
            if (questions != null)
                return Result.ok(this.questionIdsToDaos(jedis, questions));
            var result = repo.listAuctionQuestions(auctionId);
            if (result.isOk())
                Redis.setAuctionQuestions(jedis, auctionId, result.value().stream().map(QuestionDAO::getId).toList());
            return result;
        }
    }

    @Override
    public Result<BidDAO, ServiceError> getBid(String id) {
        try (var jedis = jedisPool.getResource()) {
            var bid = Redis.getBid(jedis, id);
            if (bid != null)
                return Result.ok(bid);
            var result = repo.getBid(id);
            if (result.isOk())
                Redis.setBid(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<BidDAO, ServiceError> insertBid(BidDAO bid) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.insertBid(bid);
            if (result.isOk())
                Redis.setBid(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<BidDAO, ServiceError> getTopBid(String auctionId) {
        return this.repo.getTopBid(auctionId);
    }

    @Override
    public Result<List<BidDAO>, ServiceError> listAuctionBids(String auctionId) {
        try (var jedis = jedisPool.getResource()) {
            var bids = Redis.getAuctionBids(jedis, auctionId);
            if (bids != null)
                return Result.ok(this.bidIdsToDaos(jedis, bids));
            var result = repo.listAuctionBids(auctionId);
            if (result.isOk())
                Redis.setAuctionBids(jedis, auctionId, result.value().stream().map(BidDAO::getId).toList());
            return result;
        }
    }

    @Override
    public Result<AuctionDAO, ServiceError> getAuction(String id) {
        try (var jedis = jedisPool.getResource()) {
            var auction = Redis.getAuction(jedis, id);
            if (auction != null)
                return Result.ok(auction);
            var result = repo.getAuction(id);
            if (result.isOk())
                Redis.setAuction(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<AuctionDAO, ServiceError> insertAuction(AuctionDAO auction) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.insertAuction(auction);
            if (result.isError())
                return result;

            var auctionDao = result.value();
            Redis.setAuction(jedis, auctionDao);
            Redis.addUserAuction(jedis, auction.getUserId(), auction.getId());

            return result;
        }
    }

    @Override
    public Result<AuctionDAO, ServiceError> updateAuction(AuctionDAO auction) {
        try (var jedis = jedisPool.getResource()) {
            var result = repo.updateAuction(auction);
            if (result.isOk())
                Redis.setAuction(jedis, result.value());
            return result;
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listUserAuctions(String userId, boolean open) {
        try (var jedis = jedisPool.getResource()) {
            var auctions = Redis.getUserAuctions(jedis, userId);
            if (auctions != null) {
                var auctionDaos = this.auctionIdsToDaos(jedis, auctions)
                        .stream()
                        .filter(a -> !open || a.getStatus().equals(AuctionDAO.Status.OPEN) == open)
                        .toList();
                logger.fine("Found " + auctionDaos.size() + " auctions in cache");
                return Result.ok(auctionDaos);
            }

            logger.fine("No auctions found in cache for user " + userId);
            var result = repo.listUserAuctions(userId, false);
            if (result.isOk())
                Redis.setUserAuctions(jedis, userId, result.value().stream().map(AuctionDAO::getId).toList());
            return result;
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listAuctionsFollowedByUser(String userId) {
        try (var jedis = jedisPool.getResource()) {
            var auctions = Redis.getUserAuctions(jedis, userId);
            if (auctions != null)
                return Result.ok(this.auctionIdsToDaos(jedis, auctions));
            var result = repo.listAuctionsFollowedByUser(userId);
            if (result.isOk())
                Redis.setUserAuctions(jedis, userId, result.value().stream().map(AuctionDAO::getId).toList());
            return result;
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listAuctionsAboutToClose() {
        try (var jedis = jedisPool.getResource()) {
            var auctionIds = Redis.getAuctionsAboutToClose(jedis);
            var auctionDaos = this.auctionIdsToDaos(jedis, auctionIds);
            return Result.ok(auctionDaos);
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listRecentAuctions() {
        try (var jedis = jedisPool.getResource()) {
            var auctionIds = Redis.getRecentAuctions(jedis);
            var auctionDaos = this.auctionIdsToDaos(jedis, auctionIds);
            return Result.ok(auctionDaos);
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listPopularAuctions() {
        try (var jedis = jedisPool.getResource()) {
            var auctionIds = Redis.getPopularAuctions(jedis);
            var auctionDaos = this.auctionIdsToDaos(jedis, auctionIds);
            return Result.ok(auctionDaos);
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> queryAuctions(String query) {
        return repo.queryAuctions(query);
    }

    @Override
    public Result<List<QuestionDAO>, ServiceError> queryQuestionsFromAuction(String auctionId, String query) {
        return repo.queryQuestionsFromAuction(auctionId, query);
    }

    private List<AuctionDAO> auctionIdsToDaos(Jedis jedis, List<String> ids) {
        var auctions = new ArrayList<AuctionDAO>();
        for (var id : ids) {
            var auction = Redis.getAuction(jedis, id);
            if (auction != null) {
                auctions.add(auction);
            } else {
                var result = repo.getAuction(id);
                if (result.isOk()) {
                    Redis.setAuction(jedis, result.value());
                    auctions.add(result.value());
                }
            }
        }
        return auctions;
    }

    private List<BidDAO> bidIdsToDaos(Jedis jedis, List<String> bidIds) {
        var bids = new ArrayList<BidDAO>(bidIds.size());
        for (var bidId : bidIds) {
            var bid = Redis.getBid(jedis, bidId);
            if (bid != null) {
                bids.add(bid);
            } else {
                var result = repo.getBid(bidId);
                if (result.isOk()) {
                    Redis.setBid(jedis, result.value());
                    bids.add(result.value());
                }
            }
        }
        return bids;
    }

    private List<QuestionDAO> questionIdsToDaos(Jedis jedis, List<String> questionIds) {
        var questions = new ArrayList<QuestionDAO>(questionIds.size());
        for (var questionId : questionIds) {
            var question = Redis.getQuestion(jedis, questionId);
            if (question != null) {
                questions.add(question);
            } else {
                var result = repo.getQuestion(questionId);
                if (result.isOk()) {
                    Redis.setQuestion(jedis, result.value());
                    questions.add(result.value());
                }
            }
        }
        return questions;
    }
}
