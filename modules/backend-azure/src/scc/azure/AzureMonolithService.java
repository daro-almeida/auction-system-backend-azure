package scc.azure;

import java.util.List;
import java.util.logging.Logger;

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
import scc.azure.config.AzureConfig;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.item.UserItem;

/**
 * Azure implementation of the Auction service.
 * This is responsible for authentication and validating input.
 */
public class AzureMonolithService implements UserService, MediaService, AuctionService {
    private static final Logger logger = Logger.getLogger(AzureMonolithService.class.getName());

    private final UserService userService;
    private final MediaService mediaService;
    private final AuctionService auctionService;

    public AzureMonolithService(AzureConfig config) {
        var jedisPool = Azure.createJedisPool(config.getRedisConfig());
        var database = Azure.createCosmosDatabase(config.getCosmosDbConfig());

        this.userService = new AzureUserService(config, jedisPool, database);
        this.mediaService = new AzureMediaService(config);
        this.auctionService = new AzureAuctionService(config, jedisPool, database);
    }

    @Override
    public Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params) {
        return this.auctionService.createAuction(token, params);
    }

    @Override
    public Result<AuctionItem, ServiceError> getAuction(String auctionId) {
        return this.auctionService.getAuction(auctionId);
    }

    @Override
    public Result<Void, ServiceError> updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops) {
        return this.auctionService.updateAuction(token, auctionId, ops);
    }

    @Override
    public Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params) {
        return this.auctionService.createBid(token, params);
    }

    @Override
    public Result<List<BidItem>, ServiceError> listAuctionBids(String auctionId) {
        return this.auctionService.listAuctionBids(auctionId);
    }

    @Override
    public Result<QuestionItem, ServiceError> createQuestion(SessionToken token, CreateQuestionParams params) {
        return this.auctionService.createQuestion(token, params);
    }

    @Override
    public Result<ReplyItem, ServiceError> createReply(SessionToken token, CreateReplyParams params) {
        return this.auctionService.createReply(token, params);
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> listAuctionQuestions(String auctionId) {
        return this.auctionService.listAuctionQuestions(auctionId);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId, boolean open) {
        return this.auctionService.listAuctionsOfUser(userId, open);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsFollowedByUser(String userId) {
        return this.auctionService.listAuctionsFollowedByUser(userId);
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose() {
        return this.auctionService.listAuctionsAboutToClose();
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listRecentAuctions() {
        return this.auctionService.listRecentAuctions();
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listPopularAuctions() {
        return this.auctionService.listPopularAuctions();
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> queryAuctions(String query) {
        return this.auctionService.queryAuctions(query);
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> queryQuestionsFromAuction(String auctionId, String query) {
        return this.auctionService.queryQuestionsFromAuction(auctionId, query);
    }

    @Override
    public Result<MediaId, ServiceError> uploadMedia(MediaNamespace namespace, byte[] contents) {
        return this.mediaService.uploadMedia(namespace, contents);
    }

    @Override
    public Result<byte[], ServiceError> downloadMedia(MediaId mediaId) {
        return this.mediaService.downloadMedia(mediaId);
    }

    @Override
    public Result<Void, ServiceError> deleteMedia(MediaId mediaId) {
        return this.mediaService.deleteMedia(mediaId);
    }

    @Override
    public Result<UserItem, ServiceError> createUser(CreateUserParams params) {
        return this.userService.createUser(params);
    }

    @Override
    public Result<UserItem, ServiceError> getUser(String userId) {
        return this.userService.getUser(userId);
    }

    @Override
    public Result<UserItem, ServiceError> deleteUser(SessionToken token, String userId) {
        return this.userService.deleteUser(token, userId);
    }

    @Override
    public Result<UserItem, ServiceError> updateUser(SessionToken token, String userId, UpdateUserOps ops) {
        return this.userService.updateUser(token, userId, ops);
    }

    @Override
    public Result<SessionToken, ServiceError> authenticateUser(String userId, String password) {
        return this.userService.authenticateUser(userId, password);
    }

}
