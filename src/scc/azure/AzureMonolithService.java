package scc.azure;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosPatchOperations;

import scc.azure.config.AzureMonolithConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;
import scc.services.AuctionService;
import scc.services.MediaService;
import scc.services.UserService;
import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.services.data.ReplyItem;
import scc.utils.Hash;
import scc.utils.Result;

public class AzureMonolithService implements UserService, MediaService, AuctionService {
    private final AzureMonolithConfig config;
    private final MediaStorage mediaStorage;
    private final UserDB userDB;
    private final AuctionDB auctionDB;
    private final BidDB bidDB;
    private final QuestionDB questionDB;

    public AzureMonolithService(AzureMonolithConfig config) {
        this.config = config;

        this.mediaStorage = new MediaStorage(config.getBlobStoreConfig());

        var cosmosConfig = config.getCosmosDbConfig();
        var cosmosClient = new CosmosClientBuilder()
                .endpoint(cosmosConfig.dbUrl)
                .key(cosmosConfig.dbKey)
                // .directMode()
                .gatewayMode()
                // replace by .directMode() for better performance
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        var dbClient = cosmosClient.getDatabase(cosmosConfig.dbName);

        this.userDB = new UserDB(dbClient, cosmosConfig);
        this.auctionDB = new AuctionDB(dbClient, cosmosConfig);
        this.bidDB = new BidDB(dbClient, cosmosConfig);
        this.questionDB = new QuestionDB(dbClient, cosmosConfig);
    }

    @Override
    public Result<String, scc.services.AuctionService.Error> createAuction(CreateAuctionParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.error(scc.services.AuctionService.Error.USER_NOT_FOUND);

        // TODO: Create auction picture
        var auctionDao = new AuctionDAO(params.title(), params.description(), null, params.userId(), new Date(),
                params.initialPrice());
        var response = this.auctionDB.createAuction(auctionDao);

        return response.map(AuctionDAO::getId);
    }

    @Override
    public Result<Void, scc.services.AuctionService.Error> deleteAuction(String auctionId) {
        var response = this.auctionDB.deleteAuction(auctionId);
        return response;
    }

    @Override
    public Result<Void, scc.services.AuctionService.Error> updateAuction(String auctionId, UpdateAuctionOps ops) {
        var patchOps = CosmosPatchOperations.create();
        if (ops.shouldUpdateTitle())
            patchOps.set("/title", ops.getTitle());
        if (ops.shouldUpdateDescription())
            patchOps.set("/description", ops.getDescription());
        // TODO: Update auction picture

        var response = this.auctionDB.updateAuction(auctionId, patchOps);

        return response;
    }

    @Override
    public Result<String, scc.services.AuctionService.Error> createBid(CreateBidParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.error(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.error(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        var bidDao = new BidDAO(params.auctionId(), params.userId(), params.price());
        var response = this.bidDB.createBid(bidDao);

        return response.map(BidDAO::getBidId);
    }

    @Override
    public Result<List<BidItem>, scc.services.AuctionService.Error> listBids(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.error(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        return this.bidDB.listBids(auctionId).stream()
                .map(bid -> new BidItem(bid.getBidId(), bid.getUserId(), bid.getAmount()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Result::ok));
    }

    @Override
    public Result<String, scc.services.AuctionService.Error> createQuestion(CreateQuestionParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.error(scc.services.AuctionService.Error.USER_NOT_FOUND);
        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.error(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        var questionDao = new QuestionDAO(params.auctionId(), params.userId(), params.question());
        var response = this.questionDB.createQuestion(questionDao);

        return response.map(QuestionDAO::getQuestionId);
    }

    @Override
    public Result<Void, scc.services.AuctionService.Error> createReply(CreateReplyParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.error(scc.services.AuctionService.Error.USER_NOT_FOUND);

        var response = this.questionDB.createReply(params.questionId(),
                new QuestionDAO.Reply(params.userId(), params.reply()));

        if (response.isErr())
            return Result.error(response.unwrapErr());

        return Result.ok();
    }

    @Override
    public Result<List<QuestionItem>, scc.services.AuctionService.Error> listQuestions(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.error(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        return this.questionDB.listQuestions(auctionId).stream()
                .map(question -> new QuestionItem(question.getQuestionId(), question.getUserId(),
                        question.getQuestion(),
                        Optional.ofNullable(question.getReply())
                                .map(reply -> new ReplyItem(reply.getUserId(), reply.getReply()))))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Result::ok));
    }

    @Override
    public String uploadMedia(byte[] contents) {
        return this.mediaStorage.uploadAuctionMedia(contents);
    }

    @Override
    public String uploadUserProfilePicture(String userId, byte[] contents) {
        return this.mediaStorage.uploadUserMedia(contents);
    }

    @Override
    public Optional<byte[]> downloadMedia(String id) {
        return this.mediaStorage.downloadMedia(id);
    }

    @Override
    public boolean deleteMedia(String id) {
        return this.mediaStorage.deleteMedia(id);
    }

    @Override
    public Result<String, UserService.Error> createUser(CreateUserParams params) {
        // TODO: Create photo. Only upload photo after user is created.
        var userDao = new UserDAO(params.nickname(), params.name(), Hash.of(params.password()), null);
        var result = this.userDB.createUser(userDao);
        return result.map(UserDAO::getId);
    }

    @Override
    public Result<Void, UserService.Error> deleteUser(String userId) {
        var result = this.userDB.deleteUser(userId);
        if (!result.isOk())
            return result;
        // TODO: Delete user profile picture
        return result;
    }

    @Override
    public Result<Void, UserService.Error> updateUser(String userId, UpdateUserOps ops) {
        var cosmosOps = CosmosPatchOperations.create();
        if (ops.shouldUpdateName())
            cosmosOps.set("/name", ops.getName());
        if (ops.shouldUpdatePassword())
            cosmosOps.set("/password", Hash.of(ops.getPassword()));
        // TODO: update profile picture

        return this.userDB.updateUser(userId, cosmosOps);
    }

}
