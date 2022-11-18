package scc.azure;

import java.util.ArrayList;
import java.util.List;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;

import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.QuestionDAO.Reply;
import scc.azure.dao.UserDAO;
import scc.azure.repo.AuctionRepo;
import scc.azure.repo.BidRepo;
import scc.azure.repo.QuestionRepo;
import scc.azure.repo.UserRepo;

public class CosmosRepo implements AuctionRepo, BidRepo, QuestionRepo, UserRepo {

    private final CosmosContainer auctionContainer;
    private final CosmosContainer bidContainer;
    private final CosmosContainer questionContainer;
    private final CosmosContainer userContainer;
    private final JedisPool jedisPool;

    public CosmosRepo(CosmosDbConfig config, CosmosDatabase db, JedisPool jedisPool) {
        this.auctionContainer = db.getContainer(config.auctionContainer);
        this.bidContainer = db.getContainer(config.bidContainer);
        this.questionContainer = db.getContainer(config.questionContainer);
        this.userContainer = db.getContainer(config.userContainer);
        this.jedisPool = jedisPool;
    }

    @Override
    public Result<UserDAO, ServiceError> getUser(String id) {
        var user = Cosmos.getUser(auctionContainer, id);
        if (user.isEmpty())
            return Result.err(ServiceError.USER_NOT_FOUND);
        return Result.ok(user.get());
    }

    @Override
    public Result<UserDAO, ServiceError> insertUser(UserDAO user) {
        return Cosmos.createUser(userContainer, user);
    }

    @Override
    public Result<UserDAO, ServiceError> updateUser(UserDAO user) {
        return Cosmos.updateUser(userContainer, user.getId(), user);
    }

    @Override
    public Result<UserDAO, ServiceError> deleteUser(String id) {
        return Cosmos.deleteUser(userContainer, id);
    }

    @Override
    public Result<QuestionDAO, ServiceError> getQuestion(String id) {
        var question = Cosmos.getQuestion(questionContainer, id);
        if (question.isEmpty())
            return Result.err(ServiceError.QUESTION_NOT_FOUND);
        return Result.ok(question.get());
    }

    @Override
    public Result<QuestionDAO, ServiceError> insertQuestion(QuestionDAO question) {
        return Cosmos.createQuestion(questionContainer, question);
    }

    @Override
    public Result<QuestionDAO, ServiceError> insertReply(String id, Reply reply) {
        return Cosmos.createReply(questionContainer, id, reply);
    }

    @Override
    public Result<List<QuestionDAO>, ServiceError> listAuctionQuestions(String auctionId) {
        return Cosmos.listQuestionsOfAuction(questionContainer, auctionId);
    }

    @Override
    public Result<BidDAO, ServiceError> getBid(String id) {
        var bid = Cosmos.getBid(bidContainer, id);
        if (bid.isEmpty())
            return Result.err(ServiceError.BID_NOT_FOUND);
        return Result.ok(bid.get());
    }

    @Override
    public Result<BidDAO, ServiceError> insertBid(BidDAO bid) {
        return Cosmos.createBid(bidContainer, bid);
    }

    @Override
    public Result<BidDAO, ServiceError> getTopBid(String auctionId) {
        return Result.err(ServiceError.INTERNAL_ERROR, "Not implemented");
    }

    @Override
    public Result<List<BidDAO>, ServiceError> listAuctionBids(String auctionId) {
        return Cosmos.listBidsOfAuction(bidContainer, auctionId);
    }

    @Override
    public Result<AuctionDAO, ServiceError> getAuction(String id) {
        var auction = Cosmos.getAuction(auctionContainer, id);
        if (auction.isEmpty())
            return Result.err(ServiceError.AUCTION_NOT_FOUND);
        return Result.ok(auction.get());
    }

    @Override
    public Result<AuctionDAO, ServiceError> insertAuction(AuctionDAO auction) {
        return Cosmos.createAuction(auctionContainer, auction);
    }

    @Override
    public Result<AuctionDAO, ServiceError> updateAuction(AuctionDAO auction) {
        return Cosmos.updateAuction(auctionContainer, auction.getId(), auction);
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listUserAuctions(String userId, boolean open) {
        return Cosmos.listAuctionsOfUser(auctionContainer, userId, open);
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listAuctionsFollowedByUser(String userId) {
        var listResult = Cosmos.listAuctionsFollowedByUser(bidContainer, auctionContainer, userId);
        if (listResult.isError())
            return Result.err(listResult.error());

        var auctionsIds = listResult.value();
        var auctionDaos = new ArrayList<AuctionDAO>(auctionsIds.size());

        for (var auctionId : auctionsIds) {
            var auctionResult = getAuction(auctionId);
            if (auctionResult.isError())
                return Result.err(auctionResult.error());
            auctionDaos.add(auctionResult.value());
        }

        return Result.ok(auctionDaos);
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listAuctionsAboutToClose() {
        try (var jedis = jedisPool.getResource()) {
            var auctionIds = Redis.getAuctionsAboutToClose(jedis);
            var auctionDaos = this.auctionIdsToDaos(auctionIds);
            return Result.ok(auctionDaos);
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listRecentAuctions() {
        try (var jedis = jedisPool.getResource()) {
            var auctionIds = Redis.getRecentAuctions(jedis);
            var auctionDaos = this.auctionIdsToDaos(auctionIds);
            return Result.ok(auctionDaos);
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listPopularAuctions() {
        try (var jedis = jedisPool.getResource()) {
            var auctionIds = Redis.getPopularAuctions(jedis);
            var auctionDaos = this.auctionIdsToDaos(auctionIds);
            return Result.ok(auctionDaos);
        }
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> queryAuctions(String query) {
        // TODO: Add cognitive search
        return null;
    }

    private List<AuctionDAO> auctionIdsToDaos(List<String> ids) {
        var auctionDaos = new ArrayList<AuctionDAO>(ids.size());

        for (var auctionId : ids) {
            var auctionResult = getAuction(auctionId);
            if (auctionResult.isError())
                return null;
            auctionDaos.add(auctionResult.value());
        }

        return auctionDaos;
    }
}
