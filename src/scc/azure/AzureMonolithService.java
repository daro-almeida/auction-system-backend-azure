package scc.azure;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
    private final MediaStorage mediaStorage;
    private final UserDB userDB;
    private final AuctionDB auctionDB;
    private final BidDB bidDB;
    private final QuestionDB questionDB;

    public AzureMonolithService(AzureMonolithConfig config) {
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

    /**
     * Creates an auction
     * @param params JSON that contains the necessary information to create an auction
     * @return 200 with auction's identifier if successful, 
     */
    @Override
    public Result<String, scc.services.AuctionService.Error> createAuction(CreateAuctionParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(scc.services.AuctionService.Error.USER_NOT_FOUND);

        AtomicReference<String> pictureId = new AtomicReference<>(null);
        params.image().ifPresent(img -> pictureId.set(this.mediaStorage.createAuctionMediaID(img)));

        var auctionDao = new AuctionDAO(params.title(), params.description(), pictureId.get(), params.userId(), new Date(),
                params.initialPrice());
        var response = this.auctionDB.createAuction(auctionDao);

        if(response.isOk() && params.image().isPresent())
            uploadMedia(params.image().get());

        return response.map(AuctionDAO::getId);
    }

    @Override
    public Result<Void, scc.services.AuctionService.Error> deleteAuction(String auctionId) {
        return this.auctionDB.deleteAuction(auctionId);
    }

    @Override
    public Result<Void, scc.services.AuctionService.Error> updateAuction(String auctionId, UpdateAuctionOps ops) {
        var patchOps = CosmosPatchOperations.create();
        if (ops.shouldUpdateTitle())
            patchOps.set("/title", ops.getTitle());
        if (ops.shouldUpdateDescription())
            patchOps.set("/description", ops.getDescription());
        if (ops.shouldUpdateImage()) {
            String pictureId = this.mediaStorage.createUserMediaID(ops.getImage());
            patchOps.set("/pictureId", pictureId);
        }
        var result = this.auctionDB.updateAuction(auctionId, patchOps);

        if (result.isOk())
            uploadMedia(ops.getImage());

        return result;
    }

    @Override
    public Result<String, scc.services.AuctionService.Error> createBid(CreateBidParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        var bidDao = new BidDAO(params.auctionId(), params.userId(), params.price());
        var response = this.bidDB.createBid(bidDao);

        return response.map(BidDAO::getBidId);
    }

    @Override
    public Result<List<BidItem>, scc.services.AuctionService.Error> listBids(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        return this.bidDB.listBids(auctionId).stream()
                .map(bid -> new BidItem(bid.getBidId(), bid.getUserId(), bid.getAmount()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Result::ok));
    }

    @Override
    public Result<String, scc.services.AuctionService.Error> createQuestion(CreateQuestionParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(scc.services.AuctionService.Error.USER_NOT_FOUND);
        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        var questionDao = new QuestionDAO(params.auctionId(), params.userId(), params.question());
        var response = this.questionDB.createQuestion(questionDao);

        return response.map(QuestionDAO::getQuestionId);
    }

    @Override
    public Result<Void, scc.services.AuctionService.Error> createReply(CreateReplyParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(scc.services.AuctionService.Error.USER_NOT_FOUND);

        var response = this.questionDB.createReply(params.questionId(),
                new QuestionDAO.Reply(params.userId(), params.reply()));

        if (response.isErr())
            return Result.err(response.error());

        return Result.ok();
    }

    @Override
    public Result<List<QuestionItem>, scc.services.AuctionService.Error> listQuestions(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

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
        AtomicReference<String> photoId = new AtomicReference<>(null);
        params.image().ifPresent(img -> photoId.set(this.mediaStorage.createUserMediaID(img)));
        var userDao = new UserDAO(params.nickname(), params.name(), Hash.of(params.password()), photoId.get());
        var result = this.userDB.createUser(userDao);
        if (result.isOk() && params.image().isPresent())
            uploadMedia(params.image().get());
        return result.map(UserDAO::getId);
    }


    @Override
    public Result<Void, UserService.Error> deleteUser(String userId) {
        var result = this.userDB.deleteUser(userId);
        if (!result.isOk())
            return Result.err(result.error());
        String photoId = result.value().getPhotoId();
        if (!userDB.userWithPhoto(photoId))
            deleteMedia(photoId);

        auctionDB.deleteUserAuctions(userId);
        // TODO set user in user bids as DELETED USER
        // TODO Delete the question/reply entries from this user

        return Result.ok();
    }

    @Override
    public Result<Void, UserService.Error> updateUser(String userId, UpdateUserOps ops) {
        var patchOps = CosmosPatchOperations.create();
        if (ops.shouldUpdateName())
            patchOps.set("/name", ops.getName());
        if (ops.shouldUpdatePassword())
            patchOps.set("/password", Hash.of(ops.getPassword()));
        if (ops.shouldUpdateImage()) {
            String photoId = this.mediaStorage.createUserMediaID(ops.getImage());
            patchOps.set("/photoId", photoId);
        }
        var result = this.userDB.updateUser(userId, patchOps);

        if (result.isOk())
            uploadMedia(ops.getImage());

        return result;
    }

}
