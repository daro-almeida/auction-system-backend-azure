package scc.azure;

import scc.azure.config.AzureMonolithConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.DAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;
import scc.azure.repo.AuctionRepo;
import scc.azure.repo.BidRepo;
import scc.azure.repo.QuestionRepo;
import scc.azure.repo.UserRepo;
import scc.AuctionService;
import scc.MediaId;
import scc.MediaNamespace;
import scc.MediaService;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateAuctionOps;
import scc.UpdateUserOps;
import scc.UserService;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.item.UserItem;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureMonolithService implements UserService, MediaService, AuctionService {
    private final Auth auth;
    private final AuctionRepo auctionRepo;
    private final BidRepo bidRepo;
    private final QuestionRepo questionRepo;
    private final UserRepo userRepo;
    private final MediaStorage mediaStorage;

    public AzureMonolithService(AzureMonolithConfig config) {
        var cosmosConfig = config.getCosmosDbConfig();
        var cosmosDb = Azure.createCosmosDatabase(cosmosConfig);
        var redisConfig = config.getRedisConfig();
        var jedisPool = Azure.createJedisPool(redisConfig);

        var cosmosRepo = new CosmosRepo(cosmosConfig, cosmosDb, jedisPool);
        if (config.isCachingEnabled()) {
            var cachedRepo = new CosmosRepoCache(cosmosRepo, jedisPool);
            this.auctionRepo = cachedRepo;
            this.bidRepo = cachedRepo;
            this.questionRepo = cachedRepo;
            this.userRepo = cachedRepo;
        } else {
            this.auctionRepo = cosmosRepo;
            this.bidRepo = cosmosRepo;
            this.questionRepo = cosmosRepo;
            this.userRepo = cosmosRepo;
        }
        this.auth = new Auth(this.userRepo, jedisPool);
        this.mediaStorage = new MediaStorage(config.getBlobStoreConfig());
    }

    @Override
    public Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params) {
        var authResult = this.matchUserToken(token, params.owner());
        if (authResult.isError())
            return Result.err(authResult.error());

        var result = this.auctionRepo.insertAuction(new AuctionDAO(
                params.title(),
                params.description(),
                params.mediaId().map(Azure::mediaIdToString).orElse(null),
                params.owner(),
                params.endTime(),
                params.startingPrice()));
        if (result.isError())
            return Result.err(result.error());

        var auctionDao = result.value();
        var auctionItem = DAO.auctionToItem(auctionDao, Optional.empty());

        return Result.ok(auctionItem);
    }

    @Override
    public Result<AuctionItem, ServiceError> getAuction(String auctionId) {
        var getResult = this.auctionRepo.getAuction(auctionId);
        if (getResult.isError())
            return Result.err(getResult.error());

        var result = this.auctionDaoToItem(getResult.value());
        if (result.isError())
            return Result.err(result.error());

        return Result.ok(result.value());
    }

    @Override
    public Result<Void, ServiceError> updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops) {
        var authResult = this.auth.validate(token);
        if (authResult.isError())
            return Result.err(authResult.error());
        var userId = authResult.value();

        var getResult = this.auctionRepo.getAuction(auctionId);
        if (getResult.isError())
            return Result.err(getResult.error());

        var auctionDao = getResult.value();
        if (!auctionDao.getUserId().equals(userId))
            return Result.err(ServiceError.UNAUTHORIZED);

        if (ops.shouldUpdateTitle())
            auctionDao.setTitle(ops.getTitle());
        if (ops.shouldUpdateDescription())
            auctionDao.setDescription(ops.getDescription());
        if (ops.shouldUpdateImage())
            auctionDao.setPictureId(Azure.mediaIdToString(ops.getImage()));

        var updateResult = this.auctionRepo.updateAuction(auctionDao);
        if (updateResult.isError())
            return Result.err(updateResult.error());

        return Result.ok();
    }

    @Override
    public Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params) {
        var authResult = this.matchUserToken(token, params.userId());
        if (authResult.isError())
            return Result.err(authResult.error());

        var result = this.bidRepo.insertBid(
                new BidDAO(
                        params.auctionId(),
                        params.userId(),
                        params.price(),
                        LocalDateTime.now(ZoneOffset.UTC)));
        if (result.isError())
            return Result.err(result.error());

        var bidItem = DAO.bidToItem(result.value());

        return Result.ok(bidItem);
    }

    @Override
    public Result<List<BidItem>, ServiceError> listAuctionBids(String auctionId) {
        var result = this.bidRepo.listAuctionBids(auctionId);
        if (result.isError())
            return Result.err(result.error());

        var bidItems = result.value().stream()
                .map(DAO::bidToItem)
                .collect(Collectors.toList());

        return Result.ok(bidItems);
    }

    @Override
    public Result<QuestionItem, ServiceError> createQuestion(SessionToken token, CreateQuestionParams params) {
        var authResult = this.auth.validate(token);
        if (authResult.isError())
            return Result.err(authResult.error());
        var userId = authResult.value();

        var result = this.questionRepo.insertQuestion(
                new QuestionDAO(
                        params.auctionId(),
                        userId,
                        params.question()));
        if (result.isError())
            return Result.err(result.error());

        var questionItem = DAO.questionToItem(result.value());

        return Result.ok(questionItem);
    }

    @Override
    public Result<ReplyItem, ServiceError> createReply(SessionToken token, CreateReplyParams params) {
        var authResult = this.auth.validate(token);
        if (authResult.isError())
            return Result.err(authResult.error());
        var userId = authResult.value();

        var result = this.questionRepo.insertReply(
                params.questionId(),
                new QuestionDAO.Reply(
                        userId,
                        params.reply()));
        if (result.isError())
            return Result.err(result.error());

        var questionDao = result.value();
        var replyItem = DAO.replyToItem(questionDao.getId(), questionDao.getReply());

        return Result.ok(replyItem);
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> listAuctionQuestions(String auctionId) {
        var result = this.questionRepo.listAuctionQuestions(auctionId);
        if (result.isError())
            return Result.err(result.error());

        var questionItems = result.value().stream()
                .map(DAO::questionToItem)
                .collect(Collectors.toList());

        return Result.ok(questionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId, boolean open) {
        var result = this.auctionRepo.listUserAuctions(userId, open);
        if (result.isError())
            return Result.err(result.error());

        var auctionItems = result.value().stream()
                .map(this::auctionDaoToItem)
                .filter(Result::isOk)
                .map(Result::value)
                .collect(Collectors.toList());

        return Result.ok(auctionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsFollowedByUser(String userId) {
        var result = this.auctionRepo.listAuctionsFollowedByUser(userId);
        if (result.isError())
            return Result.err(result.error());

        var auctionItems = result.value().stream()
                .map(this::auctionDaoToItem)
                .filter(Result::isOk)
                .map(Result::value)
                .collect(Collectors.toList());

        return Result.ok(auctionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose() {
        var result = this.auctionRepo.listAuctionsAboutToClose();
        if (result.isError())
            return Result.err(result.error());

        var auctionItems = result.value().stream()
                .map(this::auctionDaoToItem)
                .filter(Result::isOk)
                .map(Result::value)
                .collect(Collectors.toList());

        return Result.ok(auctionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listRecentAuctions() {
        var result = this.auctionRepo.listRecentAuctions();
        if (result.isError())
            return Result.err(result.error());

        var auctionItems = result.value().stream()
                .map(this::auctionDaoToItem)
                .filter(Result::isOk)
                .map(Result::value)
                .collect(Collectors.toList());

        return Result.ok(auctionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listPopularAuctions() {
        var result = this.auctionRepo.listPopularAuctions();
        if (result.isError())
            return Result.err(result.error());

        var auctionItems = result.value().stream()
                .map(this::auctionDaoToItem)
                .filter(Result::isOk)
                .map(Result::value)
                .collect(Collectors.toList());

        return Result.ok(auctionItems);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> queryAuctions(String query) {
        var result = this.auctionRepo.queryAuctions(query);
        if (result.isError())
            return Result.err(result.error());

        var auctionItems = result.value().stream()
                .map(this::auctionDaoToItem)
                .filter(Result::isOk)
                .map(Result::value)
                .collect(Collectors.toList());

        return Result.ok(auctionItems);
    }

    @Override
    public Result<MediaId, ServiceError> uploadMedia(MediaNamespace namespace, byte[] contents) {
        return Result.ok(this.mediaStorage.uploadMedia(namespace, contents));
    }

    @Override
    public Result<byte[], ServiceError> downloadMedia(MediaId mediaId) {
        return this.mediaStorage.downloadMedia(mediaId);
    }

    @Override
    public Result<Void, ServiceError> deleteMedia(MediaId mediaId) {
        return this.mediaStorage.deleteMedia(mediaId);
    }

    @Override
    public Result<UserItem, ServiceError> createUser(CreateUserParams params) {
        var result = this.userRepo.insertUser(new UserDAO(
                params.id(),
                params.name(),
                params.password(),
                params.imageId().map(Azure::mediaIdToString).orElse(null)));

        if (result.isError())
            return Result.err(result.error());

        var userItem = DAO.userToItem(result.value());

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> getUser(String userId) {
        var result = this.userRepo.getUser(userId);
        if (result.isError())
            return Result.err(result.error());

        var userItem = DAO.userToItem(result.value());

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> deleteUser(SessionToken token, String userId) {
        var authResult = this.matchUserToken(token, userId);
        if (authResult.isError())
            return Result.err(authResult.error());

        var result = this.userRepo.deleteUser(userId);
        if (result.isError())
            return Result.err(result.error());

        var userItem = DAO.userToItem(result.value());

        return Result.ok(userItem);
    }

    @Override
    public Result<UserItem, ServiceError> updateUser(SessionToken token, String userId, UpdateUserOps ops) {
        var authResult = this.matchUserToken(token, userId);
        if (authResult.isError())
            return Result.err(authResult.error());

        var getResult = this.userRepo.getUser(userId);
        if (getResult.isError())
            return Result.err(getResult.error());

        var userDao = getResult.value();
        if (ops.shouldUpdateName())
            userDao.setName(ops.getName());
        if (ops.shouldUpdatePassword())
            userDao.setHashedPwd(Azure.hashUserPassword(ops.getPassword()));
        if (ops.shouldUpdateImage())
            userDao.setPhotoId(Azure.mediaIdToString(ops.getImageId()));

        var updateResult = this.userRepo.updateUser(userDao);
        if (updateResult.isError())
            return Result.err(updateResult.error());

        var userItem = DAO.userToItem(updateResult.value());

        return Result.ok(userItem);
    }

    @Override
    public Result<SessionToken, ServiceError> authenticateUser(String userId, String password) {
        return this.auth.authenticate(userId, password);
    }

    private Result<AuctionItem, ServiceError> auctionDaoToItem(AuctionDAO auctionDao) {
        var topBidResult = this.bidRepo.getTopBid(auctionDao.getId());
        if (topBidResult.isError())
            return Result.err(topBidResult.error());
        var topBid = Optional.of(topBidResult.value());
        return Result.ok(DAO.auctionToItem(auctionDao, topBid));
    }

    private Result<Void, ServiceError> matchUserToken(SessionToken token, String userId) {
        var result = this.auth.validate(token);
        if (result.isError())
            return Result.err(result.error());

        var tokenUserId = result.value();
        if (!tokenUserId.equals(userId))
            return Result.err(ServiceError.UNAUTHORIZED);

        return Result.ok();
    }

}