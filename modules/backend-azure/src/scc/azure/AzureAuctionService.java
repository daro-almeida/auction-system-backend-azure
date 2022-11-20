package scc.azure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import scc.AuctionService;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateAuctionOps;
import scc.azure.config.AzureConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;

public class AzureAuctionService implements AuctionService {

    private static final Logger logger = Logger.getLogger(AzureAuctionService.class.getName());

    private final AzureConfig azureConfig;
    private final JedisPool jedisPool;
    private final CosmosContainer auctionContainer;
    private final CosmosContainer bidContainer;
    private final CosmosContainer userContainer;
    private final CosmosContainer questionContainer;
    private final SearchClient searchClientAuctions;
    private final SearchClient searchClientQuestions;
    private final Auth auth;

    public AzureAuctionService(AzureConfig config, JedisPool jedisPool, CosmosDatabase database) {
        this.azureConfig = config;
        this.jedisPool = jedisPool;
        this.auctionContainer = database.getContainer(config.getCosmosDbConfig().auctionContainer);
        this.bidContainer = database.getContainer(config.getCosmosDbConfig().bidContainer);
        this.userContainer = database.getContainer(config.getCosmosDbConfig().userContainer);
        this.questionContainer = database.getContainer(config.getCosmosDbConfig().questionContainer);

        var cogConfig = config.getCognitiveSearchConfig();
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

        this.auth = new Auth(config, jedisPool, database);
    }

    @Override
    public Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params) {
        var authResult = this.auth.match(token, params.owner());
        if (authResult.isError())
            return Result.err(authResult);

        if (params.title().isBlank() || params.description().isBlank() || params.startingPrice() <= 0)
            return Result.err(ServiceError.BAD_REQUEST);

        var auctionDao = new AuctionDAO(
                params.title(),
                params.description(),
                params.mediaId().map(Azure::mediaIdToString).orElse(null),
                params.owner(),
                Azure.formatDateTime(params.endTime()),
                params.startingPrice());

        var createResult = Cosmos.createAuction(this.auctionContainer, auctionDao);
        if (createResult.isError())
            return Result.err(createResult);

        auctionDao = createResult.value();
        var auctionItemResult = AzureData.auctionDaoToItem(
                azureConfig,
                jedisPool,
                userContainer,
                bidContainer,
                auctionDao);
        if (auctionItemResult.isError())
            return Result.err(auctionItemResult);

        var auctionItem = auctionItemResult.value();
        AzureData.setAuction(this.azureConfig, this.jedisPool, auctionDao);
        MessageBus.sendCloseAuction(auctionItem.getId(), auctionItem.getEndTime());

        return Result.ok(auctionItem);
    }

    @Override
    public Result<AuctionItem, ServiceError> getAuction(String auctionId) {
        if (auctionId.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        var auctionResult = AzureData.getAuction(this.azureConfig, this.jedisPool, this.auctionContainer, auctionId);
        if (auctionResult.isError())
            return Result.err(auctionResult);
        var auctioDao = auctionResult.value();

        var auctionItemResult = AzureData.auctionDaoToItem(
                azureConfig,
                jedisPool,
                bidContainer,
                userContainer,
                auctioDao);
        if (auctionItemResult.isError())
            return Result.err(auctionItemResult);

        return Result.ok(auctionItemResult.value());
    }

    @Override
    public Result<Void, ServiceError> updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops) {
        if (auctionId.isBlank())
            return Result.err(ServiceError.BAD_REQUEST);

        var authResult = this.auth.validate(token);
        if (authResult.isError())
            return Result.err(authResult);
        var userId = authResult.value();

        var auctionGetResult = AzureData.getAuction(this.azureConfig, this.jedisPool, this.auctionContainer, auctionId);
        if (auctionGetResult.isError())
            return Result.err(auctionGetResult);

        var auctionDao = auctionGetResult.value();
        if (!auctionDao.getUserId().equals(userId))
            return Result.err(ServiceError.UNAUTHORIZED);

        if (ops.shouldUpdateTitle())
            auctionDao.setTitle(ops.getTitle());
        if (ops.shouldUpdateDescription())
            auctionDao.setDescription(ops.getDescription());
        if (ops.shouldUpdateImage())
            auctionDao.setPictureId(Azure.mediaIdToString(ops.getImage()));

        var updateResult = Cosmos.updateAuction(this.auctionContainer, auctionId, auctionDao);
        if (updateResult.isError())
            return Result.err(updateResult);

        auctionDao = updateResult.value();
        AzureData.setAuction(this.azureConfig, this.jedisPool, auctionDao);

        return Result.ok();
    }

    @Override
    public Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params) {
        var authResult = this.auth.match(token, params.userId());
        if (authResult.isError())
            return Result.err(authResult);

        var now = LocalDateTime.now(ZoneOffset.UTC);
        var bidDao = new BidDAO(
                params.auctionId(),
                params.userId(),
                params.price(),
                Azure.formatDateTime(now));

        var createResult = Cosmos.createBid(bidContainer, bidDao);
        if (createResult.isError())
            return Result.err(createResult);
        bidDao = createResult.value();

        var auctionId = bidDao.getAuctionId();
        var auctionDaoResult = AzureData.getAuction(azureConfig, jedisPool, auctionContainer, auctionId);
        if (auctionDaoResult.isError())
            return Result.err(auctionDaoResult);
        var auctionDao = auctionDaoResult.value();

        if (!auctionDao.getStatus().equals(AuctionDAO.Status.OPEN))
            return Result.err(ServiceError.AUCTION_NOT_OPEN);

        if (auctionDao.getWinnerBidId() != null) {
            var highestBidDaoResult = AzureData.getBid(
                    azureConfig,
                    jedisPool,
                    bidContainer,
                    auctionDao.getWinnerBidId());
            if (highestBidDaoResult.isError())
                return Result.err(highestBidDaoResult.error());

            var highestBidDao = highestBidDaoResult.value();
            if (bidDao.getAmount() <= highestBidDao.getAmount())
                return Result.err(ServiceError.BID_CONFLICT);
        }

        var updateResult = Cosmos.tryUpdateAuctionBid(
                auctionContainer,
                bidContainer,
                auctionId,
                bidDao,
                Optional.ofNullable(auctionDao.getWinnerBidId()));

        if (updateResult.isError())
            return Result.err(updateResult.error());
        AzureData.invalidateAuction(azureConfig, jedisPool, auctionDao.getId());

        var userDaoResult = AzureData.getUser(this.azureConfig, this.jedisPool, this.userContainer, params.userId());
        if (userDaoResult.isError())
            return Result.err(userDaoResult);
        var userDao = userDaoResult.value();
        var bidItem = AzureData.bidDaoToItem(bidDao, userDao);

        // Update user's following auctions
        try (var jedis = jedisPool.getResource()) {
            Redis.addUserFollowedAuction(jedis, params.userId(), params.auctionId());
        }

        return Result.ok(bidItem);
    }

    @Override
    public Result<List<BidItem>, ServiceError> listAuctionBids(String auctionId) {
        var listResult = AzureData.listAuctionBids(
                azureConfig,
                jedisPool,
                bidContainer,
                auctionId);
        if (listResult.isError())
            return Result.err(listResult);

        var bidDaos = listResult.value();
        var bidItemsResult = AzureData.bidDaosToItems(
                azureConfig,
                jedisPool,
                bidContainer,
                userContainer,
                bidDaos);

        if (bidItemsResult.isError())
            return Result.err(bidItemsResult);

        return Result.ok(bidItemsResult.value());
    }

    @Override
    public Result<QuestionItem, ServiceError> createQuestion(SessionToken token, CreateQuestionParams params) {
        var authResult = this.auth.validate(token);
        if (authResult.isError())
            return Result.err(authResult);
        var userId = authResult.value();

        var questionDao = new QuestionDAO(
                params.auctionId(),
                userId,
                params.question());
        var createResult = Cosmos.createQuestion(questionContainer, questionDao);
        if (createResult.isError())
            return Result.err(createResult);

        questionDao = createResult.value();
        var questionItemResult = AzureData.questionDaoToItem(azureConfig, jedisPool, userContainer, questionDao);
        return questionItemResult;
    }

    @Override
    public Result<ReplyItem, ServiceError> createReply(SessionToken token, CreateReplyParams params) {
        var authResult = this.auth.validate(token);
        if (authResult.isError())
            return Result.err(authResult);
        var userId = authResult.value();

        var auctionId = params.auctionId();
        var auctionDaoResult = AzureData.getAuction(azureConfig, jedisPool, auctionContainer, auctionId);
        if (auctionDaoResult.isError())
            return Result.err(auctionDaoResult);
        var auctionDao = auctionDaoResult.value();

        if (!auctionDao.getUserId().equals(userId))
            return Result.err(ServiceError.UNAUTHORIZED);

        var replyDao = new QuestionDAO.Reply(
                userId,
                params.reply());
        var createResult = Cosmos.createReply(questionContainer, params.questionId(), replyDao);
        if (createResult.isError())
            return Result.err(createResult);

        var questionDao = createResult.value();
        AzureData.setQuestion(azureConfig, jedisPool, questionDao);

        var questionItemResult = AzureData.questionDaoToItem(
                azureConfig,
                jedisPool,
                userContainer,
                questionDao);
        if (questionItemResult.isError())
            return Result.err(questionItemResult);

        return Result.ok(questionItemResult.value().getReply().get());
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> listAuctionQuestions(String auctionId) {
        var listResult = AzureData.listAuctionQuestions(
                azureConfig,
                jedisPool,
                questionContainer,
                auctionId);
        if (listResult.isError())
            return Result.err(listResult);

        var questionDaos = listResult.value();
        var questionItemsResult = AzureData.questionDaosToItems(
                azureConfig,
                jedisPool,
                userContainer,
                questionDaos);

        if (questionItemsResult.isError())
            return Result.err(questionItemsResult);

        return Result.ok(questionItemsResult.value());
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId, boolean open) {
        var listResult = AzureData.listUserAuctions(
                azureConfig,
                jedisPool,
                auctionContainer,
                userId,
                open);

        if (listResult.isError())
            return Result.err(listResult);

        var auctionDaos = listResult.value();
        var auctionItemsResult = AzureData.auctionDaosToItems(
                azureConfig,
                jedisPool,
                auctionContainer,
                bidContainer,
                userContainer,
                auctionDaos);
        if (auctionItemsResult.isError())
            return Result.err(auctionItemsResult);

        var auctionItems = auctionItemsResult.value();
        return Result.ok(auctionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsFollowedByUser(String userId) {
        var auctionDaosResult = AzureData.listAuctionsFollowedByUser(
                azureConfig,
                jedisPool,
                auctionContainer,
                bidContainer,
                userId);
        if (auctionDaosResult.isError())
            return Result.err(auctionDaosResult);

        var auctionDaos = auctionDaosResult.value();
        var auctionItemsResult = AzureData.auctionDaosToItems(
                azureConfig,
                jedisPool,
                auctionContainer,
                bidContainer,
                userContainer,
                auctionDaos);
        if (auctionItemsResult.isError())
            return Result.err(auctionItemsResult);

        var auctionItems = auctionItemsResult.value();
        return Result.ok(auctionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose() {
        var auctionDaosResult = AzureData.listAuctionsAboutToClose(
                azureConfig,
                jedisPool,
                auctionContainer);
        if (auctionDaosResult.isError())
            return Result.err(auctionDaosResult);

        var auctionDaos = auctionDaosResult.value();
        var auctionItemsResult = AzureData.auctionDaosToItems(
                azureConfig,
                jedisPool,
                auctionContainer,
                bidContainer,
                userContainer,
                auctionDaos);

        if (auctionItemsResult.isError())
            return Result.err(auctionItemsResult);

        return Result.ok(auctionItemsResult.value());
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listRecentAuctions() {
        var auctionDaosResult = AzureData.listRecentAuctions(
                azureConfig,
                jedisPool,
                auctionContainer);
        if (auctionDaosResult.isError())
            return Result.err(auctionDaosResult);

        var auctionDaos = auctionDaosResult.value();
        var auctionItemsResult = AzureData.auctionDaosToItems(
                azureConfig,
                jedisPool,
                auctionContainer,
                bidContainer,
                userContainer,
                auctionDaos);

        if (auctionItemsResult.isError())
            return Result.err(auctionItemsResult);

        return Result.ok(auctionItemsResult.value());
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listPopularAuctions() {
        var auctionDaosResult = AzureData.listPopularAuctions(
                azureConfig,
                jedisPool,
                auctionContainer);
        if (auctionDaosResult.isError())
            return Result.err(auctionDaosResult);

        var auctionDaos = auctionDaosResult.value();
        var auctionItemsResult = AzureData.auctionDaosToItems(
                azureConfig,
                jedisPool,
                auctionContainer,
                bidContainer,
                userContainer,
                auctionDaos);

        if (auctionItemsResult.isError())
            return Result.err(auctionItemsResult);

        return Result.ok(auctionItemsResult.value());
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> queryAuctions(String query) {
        SearchOptions options = new SearchOptions()
                .setIncludeTotalCount(true)
                .setSelect("id")
                .setSearchFields("title", "description", "userId")
                .setTop(AzureLogic.MAX_AUCTION_QUERY_RESULTS);

        var list = new ArrayList<AuctionDAO>();
        for (SearchResult searchResult : searchClientAuctions.search(query, options, null)) {
            var doc = searchResult.getDocument(AuctionDAO.class);
            var result = AzureData.getAuction(azureConfig, jedisPool, auctionContainer, doc.getId());
            if (result.isError()) {
                logger.warning("Error getting auction returned by search: " + result.error());
                continue;
            }
            list.add(result.value());
        }

        logger.fine("Query '" + query + "' returned " + list.size() + " auctions");
        var auctionItemsResult = AzureData.auctionDaosToItems(
                azureConfig,
                jedisPool,
                auctionContainer,
                bidContainer,
                userContainer,
                list);
        if (auctionItemsResult.isError())
            return Result.err(auctionItemsResult);

        return Result.ok(auctionItemsResult.value());
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> queryQuestionsFromAuction(String auctionId, String query) {
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
            var result = AzureData.getQuestion(azureConfig, jedisPool, questionContainer, doc.getId());
            if (result.isError()) {
                logger.warning("Error getting question returned by search: " + result.error());
                continue;
            }

            var questionDao = result.value();
            if (!questionDao.getAuctionId().equals(auctionId))
                logger.warning("Question returned by search is not from the specified auction");

            list.add(result.value());
        }

        logger.fine("Query '" + query + "' returned " + list.size() + " questions");
        var questionItemsResult = AzureData.questionDaosToItems(
                azureConfig,
                jedisPool,
                userContainer,
                list);
        if (questionItemsResult.isError())
            return Result.err(questionItemsResult);

        return Result.ok(questionItemsResult.value());
    }

}
