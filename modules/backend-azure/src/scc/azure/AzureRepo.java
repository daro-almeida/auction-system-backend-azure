package scc.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;

import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import redis.clients.jedis.JedisPool;
import scc.Result;
import scc.ServiceError;
import scc.azure.config.CognitiveSearchConfig;
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

public class AzureRepo implements AuctionRepo, BidRepo, QuestionRepo, UserRepo {
    private static final Logger logger = Logger.getLogger(AzureRepo.class.getName());

    private final CosmosContainer auctionContainer;
    private final CosmosContainer bidContainer;
    private final CosmosContainer questionContainer;
    private final CosmosContainer userContainer;
    private final SearchClient searchClientAuctions;
    private final SearchClient searchClientQuestions;
    private final JedisPool jedisPool;

    public AzureRepo(CosmosDbConfig config, CosmosDatabase db, CognitiveSearchConfig cogConfig, JedisPool jedisPool) {
        this.auctionContainer = db.getContainer(config.auctionContainer);
        this.bidContainer = db.getContainer(config.bidContainer);
        this.questionContainer = db.getContainer(config.questionContainer);
        this.userContainer = db.getContainer(config.userContainer);
        this.jedisPool = jedisPool;
        this.searchClientAuctions = new SearchClientBuilder()
                .credential(new AzureKeyCredential(cogConfig.key))
                .endpoint(cogConfig.url)
                .indexName(cogConfig.auctionsIndex)
                .buildClient();
        this.searchClientQuestions = new SearchClientBuilder()
                .credential(new AzureKeyCredential(cogConfig.key))
                .endpoint(cogConfig.url)
                .indexName(cogConfig.questionsIndex)
                .buildClient();
    }

    @Override
    public Result<UserDAO, ServiceError> getUser(String id) {
        var user = Cosmos.getUser(userContainer, id);
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
        var auctionId = bid.getAuctionId();

        // Get the auction
        var auctionDaoResult = this.getAuction(auctionId);
        if (auctionDaoResult.isError())
            return Result.err(auctionDaoResult.error());
        var auctionDao = auctionDaoResult.value();

        // Make sure it is open and the bid is higher than the current price
        if (!auctionDao.getStatus().equals(AuctionDAO.Status.OPEN))
            return Result.err(ServiceError.AUCTION_NOT_OPEN);

        if (auctionDao.getWinnerBidId() != null) {
            var highestBidDaoResult = this.getBid(auctionDao.getWinnerBidId());
            if (highestBidDaoResult.isError())
                return Result.err(highestBidDaoResult.error());

            var highestBidDao = highestBidDaoResult.value();
            if (bid.getAmount() <= highestBidDao.getAmount())
                return Result.err(ServiceError.BID_CONFLICT);
        }

        // Create the bid and try to update the auction
        var bidResult = Cosmos.createBid(this.bidContainer, bid);
        if (bidResult.isError())
            return Result.err(bidResult.error());
        var bidDao = bidResult.value();

        var updateResult = Cosmos.tryUpdateAuctionBid(auctionContainer, bidContainer, auctionId, bidDao,
                Optional.ofNullable(auctionDao.getWinnerBidId()));
        if (updateResult.isError())
            return Result.err(updateResult.error());

        return Result.ok(bidDao);
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
        var result = Cosmos.createAuction(auctionContainer, auction);
        if (result.isError())
            return result;

        var auctionDao = result.value();
        var endTime = Azure.parseDateTime(auctionDao.getEndTime());
        MessageBus.sendCloseAuction(auctionDao.getId(), endTime);

        return result;
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
        var listResult = Cosmos.listAuctionsFollowedByUser(bidContainer, userId);
        if (listResult.isError())
            return Result.err(listResult.error());

        var auctionsIds = listResult.value();
        var auctionDaos = this.auctionIdsToDaos(auctionsIds);

        return Result.ok(auctionDaos);
    }

    @Override
    public Result<List<AuctionDAO>, ServiceError> listAuctionsAboutToClose() {
        return Cosmos.listAuctionsAboutToClose(this.auctionContainer, AzureLogic.MAX_ABOUT_TO_CLOSE_AUCTIONS);
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
        SearchOptions options = new SearchOptions()
                .setIncludeTotalCount(true)
                .setSelect("id")
                .setSearchFields("title", "description", "userId")
                .setTop(AzureLogic.MAX_AUCTION_QUERY_RESULTS);

        var list = new ArrayList<AuctionDAO>();
        for (SearchResult searchResult : searchClientAuctions.search(query, options, null)) {
            AuctionDAO doc = searchResult.getDocument(AuctionDAO.class);
            var result = this.getAuction(doc.getId());
            if (result.isError()) {
                logger.warning("Error getting auction returned by search: " + result.error());
                continue;
            }
            list.add(result.value());
        }

        logger.fine("Query '" + query + "' returned " + list.size() + " auctions");

        return Result.ok(list);
    }

    @Override
    public Result<List<QuestionDAO>, ServiceError> queryQuestionsFromAuction(String auctionId, String query) {
        SearchOptions options = new SearchOptions()
                .setIncludeTotalCount(true)
                .setSelect("id")
                .setSearchFields("question", "reply", "auctionId")
                .setTop(AzureLogic.MAX_QUESTION_QUERY_RESULTS);
        var searchResults = searchClientQuestions.search(
                String.format("$filter=(auctionId eq '%s')&search='%s'", auctionId, query),
                options,
                null);

        var list = new ArrayList<QuestionDAO>();
        for (var searchResult : searchResults) {
            var doc = searchResult.getDocument(QuestionDAO.class);
            var result = this.getQuestion(doc.getId());
            if (result.isError()) {
                logger.warning("Error getting question returned by search: " + result.error());
                continue;
            }

            var questionDao = result.value();
            if (!questionDao.getAuctionId().equals(auctionId))
                logger.warning("Question returned by search is not from the specified auction");

            list.add(result.value());
        }
        return Result.ok(list);
    }

    private List<AuctionDAO> auctionIdsToDaos(List<String> ids) {
        var auctionDaos = new ArrayList<AuctionDAO>(ids.size());

        for (var auctionId : ids) {
            var auctionResult = this.getAuction(auctionId);
            if (auctionResult.isError()) {
                logger.warning("Error getting auction with id " + auctionId + ": " + auctionResult.error());
                continue;
            }
            auctionDaos.add(auctionResult.value());
        }

        return auctionDaos;
    }
}
