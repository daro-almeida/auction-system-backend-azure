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

    /**
     * Creates an auction
     * @param params JSON that contains the necessary information to create an auction
     * @return 200 with auction's identifier if successful, 404 otherwise
     */
    @Override
    public Result<String, scc.services.AuctionService.Error> createAuction(CreateAuctionParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(scc.services.AuctionService.Error.USER_NOT_FOUND);
        // TODO: Sanitize input
        var mediaId = this.mediaStorage.uploadAuctionMedia(params.image().get());
        var auctionDao = new AuctionDAO(params.title(),
                params.description(),
                mediaId,
                params.userId(),
                new Date() /*new SimpleDateFormat("dd/MM/yyyy").parse(params.endTime()) */,
                params.initialPrice());
        var response = this.auctionDB.createAuction(auctionDao);

        return response.map(AuctionDAO::getId);
    }

    /**
     * Deletes the auctions from a user with given identifier which just sets the auction's status to DELETED
     * @param auctionId identifier of the auction
     * @return 204 if successful, there shouldn't be any error cases when erasing auctions
     * (If no auction, that's fine, we don't update)
     */
    @Override
    public Result<Void, scc.services.AuctionService.Error> deleteAuction(String auctionId) {
        var response = this.auctionDB.deleteAuction(auctionId);
        return response;
    }

    /**
     * Updates the auction's values to new given ones with given identifier
     * @param auctionId identifier of the auction
     * @param ops Operations to change the values of the auction
     * @return 204 if successful,
     */
    @Override
    public Result<Void, scc.services.AuctionService.Error> updateAuction(String auctionId, UpdateAuctionOps ops) {
        var patchOps = CosmosPatchOperations.create();
        if (ops.shouldUpdateTitle())
            patchOps.set("/title", ops.getTitle());
        if (ops.shouldUpdateDescription())
            patchOps.set("/description", ops.getDescription());
        if (ops.shouldUpdateImage()){
            var mediaId = this.mediaStorage.uploadAuctionMedia(ops.getImage());
            patchOps.set("/pictureId", mediaId);
        }

        var response = this.auctionDB.updateAuction(auctionId, patchOps);

        return response;
    }

    /**
     * Creates a bid to be inserted into its respective database
     * @param params Parameters required to create a bid
     * @return 200 with created bid's identifier, 404 otherwise
     */
    @Override
    public Result<String, scc.services.AuctionService.Error> createBid(CreateBidParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(AuctionService.Error.USER_NOT_FOUND);

        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);
        // TODO: Sanitize input (bid value cannot be lower than 0)
        var bidDao = new BidDAO(params.auctionId(), params.userId(), params.price());
        var response = this.bidDB.createBid(bidDao);

        return response.map(BidDAO::getBidId);
    }

    /**
     * Lists all bids made on an auction with given identifier
     * @param auctionId identifier of the auction
     * @return 200 with list of bids, 404 otherwise
     */
    @Override
    public Result<List<BidItem>, scc.services.AuctionService.Error> listBids(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);

        return this.bidDB.listBids(auctionId).stream()
                .map(bid -> new BidItem(bid.getBidId(), bid.getUserId(), bid.getAmount()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Result::ok));
    }

    /**
     * Creates a question to be inserted in the respective database
     * @param params Parameters required to create a question
     * @return 200 with created question's identifier, 404 otherwise
     */
    @Override
    public Result<String, scc.services.AuctionService.Error> createQuestion(CreateQuestionParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(scc.services.AuctionService.Error.USER_NOT_FOUND);
        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(scc.services.AuctionService.Error.AUCTION_NOT_FOUND);
        // TODO: Sanitize input (description can't be null)
        var questionDao = new QuestionDAO(params.auctionId(), params.userId(), params.question());
        var response = this.questionDB.createQuestion(questionDao);

        return response.map(QuestionDAO::getQuestionId);
    }

    /**
     * Creates a reply to be stored in its respective database
     * The reply is meant towards a question made in an auction and only the owner of that auction
     * can reply
     * @param params Parameters required to create a reply
     * @return 204 if successful, 404 if no user, 403 if reply's user identifier is not the same as auction's owner
     */
    @Override
    public Result<Void, scc.services.AuctionService.Error> createReply(CreateReplyParams params) {
        if (!this.userDB.userExists(params.userId()))
            return Result.err(scc.services.AuctionService.Error.USER_NOT_FOUND);
        // TODO: Sanitize input (description can't be null)
        var response = this.questionDB.createReply(params.questionId(),
                new QuestionDAO.Reply(params.userId(), params.reply()));

        if (response.isErr())
            return Result.err(response.error());

        return Result.ok();
    }

    /**
     * Lists all question in an auction with given identifier
     * TODO: Should it still display the questions if the user was deleted or should we just erase it completely?
     * @param auctionId identifier of the auction
     * @return 200 with list of questions, 404 otherwise
     */
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

    /**
     * Uploads a media resource's byte contents into the blob storage for auctions
     * @param contents resource's bytes
     * @return media resource's generated identifier
     */
    @Override
    public String uploadMedia(byte[] contents) {
        return this.mediaStorage.uploadAuctionMedia(contents);
    }

    /**
     * Uploads a media resource's byte contents into the blob storage for users, more specifically
     * the user with given identifier
     * @param userId identifier of the user
     * @param contents resource's bytes
     * @return media resource's generated identifier
     */
    @Override
    public String uploadUserProfilePicture(String userId, byte[] contents) {
        return this.mediaStorage.uploadUserMedia(contents);
    }

    /**
     * Downloads the contents of a media resource stored in the blob storage container with given identifier
     * Prefixes in the identifier will determine which container should it check
     * @param id identifier of the media resource
     * @return byte contents of the media resource or null if it doesn't exist
     */
    @Override
    public Optional<byte[]> downloadMedia(String id) {
        return this.mediaStorage.downloadMedia(id);
    }

    /**
     * Deletes the contents of a media resource stored in the blob storage container with given identifier
     * Prefixes in the identifier will determine whcih container should it check
     * @param id identifier of the media resource
     * @return true if deleted successfully, false otherwise
     */
    @Override
    public boolean deleteMedia(String id) {
        return this.mediaStorage.deleteMedia(id);
    }

    /**
     * Creates a User with given parameters
     * @param params Parameters required to create a user
     * @return 200 with created user's identifier, 409 if user already exists
     */
    @Override
    public Result<String, UserService.Error> createUser(CreateUserParams params) {
        var userDao = new UserDAO(params.nickname(), params.name(), Hash.of(params.password()), null);
        var result = this.userDB.createUser(userDao);
        if(result.isOk() && params.image().isPresent()){
            var mediaId = uploadUserProfilePicture(params.nickname(), params.image().get());
            var cosmosOps = CosmosPatchOperations.create();
            cosmosOps.set("/photoId", mediaId);
        }
        return result.map(UserDAO::getId);
    }

    /**
     * Deletes a user with given identifier from the database
     * Updates the status of all auctions and bids from this user to DELETED
     * @param userId identifier of the user
     * @return
     */
    @Override
    public Result<Void, UserService.Error> deleteUser(String userId) {
        var result = this.userDB.deleteUser(userId);
        if (!result.isOk())
            return result;
        // TODO: Delete user profile picture
        // TODO: Set status "DELETED" to all auctions from this user
        // TODO: Set status "DELETED" to all bids from this user / delete the bid entries from this user
        // TODO: Delete the question/reply entries from this user
        return result;
    }

    /**
     * Updates the user with given identifier's values to new given values
     * @param userId identifier of the user
     * @param ops Operations to change the values of the user
     * @return 204 if successful, 404 otherwise
     */
    @Override
    public Result<Void, UserService.Error> updateUser(String userId, UpdateUserOps ops) {
        var cosmosOps = CosmosPatchOperations.create();
        if (ops.shouldUpdateName())
            cosmosOps.set("/name", ops.getName());
        if (ops.shouldUpdatePassword())
            cosmosOps.set("/password", Hash.of(ops.getPassword()));
        if(ops.shouldUpdateImage()){
            var mediaId = this.mediaStorage.uploadUserMedia(ops.getImage());
            cosmosOps.set("/photoId", mediaId);
        }

        return this.userDB.updateUser(userId, cosmosOps);
    }

}
