package scc.azure;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosPatchOperations;

import scc.azure.cache.Cache;
import scc.azure.cache.NoOpCache;
import scc.azure.cache.RedisCache;
import scc.azure.config.AzureMonolithConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;
import scc.services.AuctionService;
import scc.services.MediaService;
import scc.services.ServiceError;
import scc.services.UserService;
import scc.services.data.AuctionItem;
import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.services.data.UserItem;
import scc.utils.Hash;
import scc.utils.Result;

public class AzureMonolithService implements UserService, MediaService, AuctionService {
    private final MediaStorage mediaStorage;
    private final UserDB userDB;
    private final AuctionDB auctionDB;
    private final BidDB bidDB;
    private final QuestionDB questionDB;
    private final UserAuth userAuth;
    private final Cache cache;

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

        var redisConfig = config.getRedisConfig();
        var jedisPool = AzureUtils.createJedisPool(redisConfig);

        if (config.isCachingEnabled())
            this.cache = new RedisCache(jedisPool);
        else
            this.cache = new NoOpCache();

        this.userAuth = new UserAuth(this.userDB, jedisPool);

        // TESTING: create an admin user
        this.userDB.createUser(new UserDAO("admin", "admin", AzureUtils.hashUserPassword("123"), null));
        jedisPool.getResource().flushAll();
    }

    /**
     * Creates an auction for a user with given identifier.
     * The user creating the auction must be logged in to execute this operation.
     * 
     * @param params JSON that contains the necessary information to create an
     *               auction
     * @param sessionToken   Cookie related to the user being "logged" in the application
     * @return 200 with auction's identifier if successful,
     *         404 if the user does not exist
     *         403 if the authentication phase failed
     */
    @Override
    public Result<AuctionItem, ServiceError> createAuction(CreateAuctionParams params, String sessionToken) {
        var validateResult = AuctionService.validateCreateAuctionParams(params);
        if (validateResult.isError())
            return Result.err(validateResult.error());

        var authResult = this.userAuth.validateSessionToken(sessionToken);
        if (authResult.isError())
            return Result.err(authResult.error());
        var userId = authResult.value();

        var pictureId = params.image().map(mediaStorage::createAuctionMediaID);
        var auctionDao = new AuctionDAO(params.title(), params.description(), pictureId.orElse(null), userId,
                new Date(), params.initialPrice());
        var response = this.auctionDB.createAuction(auctionDao);

        if (response.isOk() && params.image().isPresent())
            uploadAuctionMedia(params.image().get());

        this.cache.setAuction(response.value());
        // TODO Delete search entries from cache
        return Result.ok(AuctionItem.fromAuctionDAO(response.value()));
    }

    /**
     * Sets the auction state into deleted so, when displayed to the user, it will
     * appear "created by DELETED USER".
     * This operation does not need authentication as the DeleteUser already did it.
     * 
     * @param auctionId identifier of the auction to be deleted
     * @return 204 if deleted successfully,
     *         404 if the auction doesn't exist (do we really need to check this?)
     */
    @Override
    public Result<Void, ServiceError> deleteAuction(String auctionId) {
        var result = this.auctionDB.deleteAuction(auctionId);
        if (result.isOk()) {
            this.cache.unsetAuction(auctionId);
            // TODO Delete search entries from cache

        }
        return result;
    }

    /**
     * Lists all auctions created by a user with given identifier.
     * This operation does not need authentication as
     * any user should be able to see the auctions from any other user, even their
     * own.
     * 
     * @param userId identifier of the user
     * @return 200 with all user's auctions or empty,
     *         404 if the user does not exist
     */
    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId) {
        // TODO Get search result from cache if it exists
        // TODO If not, make the search in database and save in cache
        var result = this.auctionDB.listAuctionsOfUser(userId);
        return Result.ok(result.value().stream().map(AuctionItem::fromAuctionDAO).toList());
    }

    /**
     * Updates all the auction's values into new given ones.
     * The user who is the owner of the auction must be logged in order to execute
     * this operation.
     * 
     * @param auctionId identifier of the auction to be updated
     * @param ops       Values that are to be changed to new ones
     * @param sessionToken      Cookie related to the user being "logged" in the application
     * @return 204 if successful on changing the auction's values,
     *         404 if the auction does not exist,
     *         403 if the authentication phase failed
     */
    @Override
    public Result<Void, ServiceError> updateAuction(String auctionId, UpdateAuctionOps ops, String sessionToken) {
        var authResult = this.userAuth.validateSessionToken(sessionToken);
        if (authResult.isError())
            return Result.err(authResult.error());

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

        if (result.isOk()) {
            var updatedAuction = this.auctionDB.getAuction(auctionId).get();
            this.cache.setAuction(updatedAuction);
            uploadAuctionMedia(ops.getImage());
            // TODO Delete searches entries from cache
            // TODO Searching for lists in cache that contain this auction might be
            // exhausting
        }

        return result;
    }

    /**
     * Creates a bid in an auction with given identifier from the logged user. If
     * the bid overpowers all current ones,
     * the auction's winning bid will be set to this one's identifier
     * The user making the bid must be logged in to execute this operation
     * 
     * @param params Parameters required to create a bid object
     * @param sessionToken   Cookie related to the user being "logged" in the application
     * @return 200 with bid's generated identifier,
     *         404 if the user making the bid or the auction doesn't exist
     *         403 if the authentication phase failed
     */
    @Override
    public Result<BidItem, ServiceError> createBid(CreateBidParams params, String sessionToken) {
        var authResult = this.userAuth.validateSessionToken(sessionToken);
        if (authResult.isError())
            return Result.err(authResult.error());
        var userId = authResult.value();

        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        var bidDao = new BidDAO(params.auctionId(), userId, params.price());
        var response = this.bidDB.createBid(bidDao);

        this.cache.setBid(bidDao);
        // TODO Delete search entries from cache
        return Result.ok(BidItem.fromBidDAO(response.value()));
    }

    /**
     * Lists all bids made on an auction with given identifier.
     * This operation does not need authentication as any user should be
     * able to see any auction's bids to know whether they bid on it or not.
     * 
     * @param auctionId identifier of the auction
     * @return 200 with list of bids in the auction or empty,
     *         404 if auction doesn't exist
     */
    @Override
    public Result<List<BidItem>, ServiceError> listBids(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        //TODO Get search result from cache if it exists
        //TODO If not, make the search in database and save in cache

        var result = this.bidDB.listBids(auctionId);
        return Result.ok(result.stream().map(BidItem::fromBidDAO).toList());
    }

    /**
     * Creates a question on an auction with given identifier made by a user with
     * given identifier
     * The user making the question must be logged in to make this operation
     * 
     * @param params Parameters required to create a question
     * @param sessionToken Cookie related to the user being "logged" in the application
     * @return 200 with generated question's identifier,
     *         404 if the user or the auction does not exist,
     *         403 if the authentication phase failed
     */
    @Override
    public Result<String, ServiceError> createQuestion(CreateQuestionParams params, String sessionToken) {
        var authResult = this.userAuth.validateSessionToken(sessionToken);
        if (authResult.isError())
            return Result.err(authResult.error());
        var userId = authResult.value();

        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        var questionDao = new QuestionDAO(params.auctionId(), userId, params.question());
        var response = this.questionDB.createQuestion(questionDao);

        // Store in cache
        this.cache.setQuestion(questionDao);
        // TODO Delete search entries from cache
        return response.map(QuestionDAO::getId);
    }

    /**
     * Creates a reply destined towards a specific question made in a given auction
     * that is answered by the owner
     * The user who is owner of the auction must be logged in to execute this
     * operation
     * 
     * @param params Parameters required to create a reply
     * @param sessionToken   Cookie related to the user being "logged" in the application
     * @return 200 with generated reply's identifier,
     *         404 if the user or auction does not exist,
     *         403 if the authentication phase failed
     */
    @Override
    public Result<Void, ServiceError> createReply(CreateReplyParams params, String sessionToken) {
        var authResult = this.userAuth.validateSessionToken(sessionToken);
        if (authResult.isError())
            return Result.err(authResult.error());
        var userId = authResult.value();

        var response = this.questionDB.createReply(params.questionId(),
                new QuestionDAO.Reply(userId, params.reply()));

        if (response.isError())
            return Result.err(response.error());

        var repliedQuestion = response.value();
        this.cache.setQuestion(repliedQuestion);
        // TODO Delete search entries from cache
        return Result.ok();
    }

    /**
     * Lists all the questions made in an auction with given identifier
     * 
     * @param auctionId identifier of the auction
     * @return 200 with the questions made in the auction or empty,
     *         404 if the auction does not exist
     */
    @Override
    public Result<List<QuestionItem>, ServiceError> listQuestions(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        var questionIds = this.cache.getAuctionQuestions(auctionId);
        if (questionIds != null) {
            var questionDaos = new ArrayList<QuestionDAO>(questionIds.size());
            for (var questionId : questionIds) {
                var cachedQuestionDao = this.cache.getQuestion(questionId);
                if (cachedQuestionDao != null) {
                    questionDaos.add(cachedQuestionDao);
                } else {
                    var questionDao = this.questionDB.getQuestion(questionId);
                    if (questionDao.isPresent()) {
                        this.cache.setQuestion(questionDao.get());
                        questionDaos.add(questionDao.get());
                    }
                }
            }
            var questionItems = questionDaos.stream().map(AzureUtils::questionDaoToItem).toList();
            return Result.ok(questionItems);
        } else {
            var questionDaos = this.questionDB.listQuestions(auctionId);
            var questionItems = questionDaos.stream().map(AzureUtils::questionDaoToItem).toList();
            questionDaos.forEach(this.cache::setQuestion);
            questionDaos.stream().map(QuestionDAO::getId).forEach(id -> this.cache.addAuctionQuestion(auctionId, id));
            return Result.ok(questionItems);
        }
    }

    /**
     * Uploads a media resource into the blob storage
     * 
     * @param contents bytes of the media resource
     * @return Uploaded media resource's generated identifier
     */
    @Override
    public String uploadAuctionMedia(byte[] contents) {
        return this.mediaStorage.uploadAuctionMedia(contents);
    }

    /**
     * Uploads a media resource destined for a user with given identifier
     * 
     * @param userId   identifier of the user
     * @param contents bytes of the media resource
     * @return Uploaded media resource's generated identifier
     */
    @Override
    public String uploadUserProfilePicture(String userId, byte[] contents) {
        return this.mediaStorage.uploadUserMedia(contents);
    }

    /**
     * Downloads the contents of a media resource with given identifier stored in
     * the blob storage
     * 
     * @param id identifier of the media resource
     * @return bytes of the associated media resource
     */
    @Override
    public Optional<byte[]> downloadMedia(String id) {
        return this.mediaStorage.downloadMedia(id);
    }

    /**
     * Deletes the contents of a media resource with given identifier from the blob
     * storage
     * 
     * @param id identifier of the media resource
     * @return true if it was deleted, false otherwise
     */
    @Override
    public boolean deleteMedia(String id) {
        return this.mediaStorage.deleteMedia(id);
    }

    /**
     * Creates a user with given values to be stored in the application's user
     * database.
     * This operation does not need authentication as anyone outside the application
     * should be able to register
     * to the application.
     * 
     * @param params Parameters required to create a user object
     * @return 200 with generated user's identifier,
     *         409 if the user with given identifier already exists
     */
    @Override
    public Result<UserItem, ServiceError> createUser(CreateUserParams params) {
        var validateResult = UserService.validateCreateUserParams(params);
        if (validateResult.isError())
            return Result.err(validateResult.error());

        var photoId = params.image().map(this.mediaStorage::createUserMediaID);
        var userDao = new UserDAO(params.nickname(), params.name(), Hash.of(params.password()), photoId.orElse(null));
        var result = this.userDB.createUser(userDao);
        if (result.isError())
            return Result.err(result.error());
        if (params.image().isPresent())
            uploadAuctionMedia(params.image().get());

        this.cache.setUser(result.value());

        return Result.ok(UserItem.fromUserDAO(result.value()));
    }

    /**
     * Deletes a user with given identifier from the application's user database.
     * The user logged in must be the same as the one from the request and, after
     * the operation is executed,
     * we make the cookie invalid.
     * All the auctions associated with this user must be in the state of deleted as
     * in, when
     * performing reads on any of them, we show that it was created by a "DELETED
     * USER".
     * 
     * @param userId identifier of the user
     * @param sessionToken   Cookie related to the user being "logged" in the application
     * @return 204 if deleted successfully,
     *         403 if the authentication phase failed
     */
    @Override
    public Result<Void, ServiceError> deleteUser(String userId, String sessionToken) {
        var authResult = this.userAuth.validateSessionToken(sessionToken);
        if (authResult.isError())
            return Result.err(authResult.error());

        var result = this.userDB.deleteUser(userId);
        if (result.isError())
            return Result.err(result.error());

        String photoId = result.value().getPhotoId();
        if (!userDB.userWithPhoto(photoId))
            deleteMedia(photoId);

        auctionDB.deleteUserAuctions(userId);

        questionDB.deleteQuestionFromUser(userId);
        bidDB.deleteBidsFromUser(userId);
        userAuth.deleteSessionToken(sessionToken);

        this.cache.unsetUser(userId);
        // TODO Delete search entries from cache
        return Result.ok();
    }

    /**
     * Updates the values stored in a user with given identifier to new ones
     * The updated user must be logged in to execute this operation
     * 
     * @param userId identifier of the user
     * @param ops    Values to be placed on the user
     * @param sessionToken   Cookie related to the user being "logged" in the application
     * @return 204 if updated successfully,
     *         404 if the user does not exist,
     *         403 if the authentication phase failed
     *         400 if the request is not well-formed
     */
    @Override
    public Result<Void, ServiceError> updateUser(String userId, UpdateUserOps ops, String sessionToken) {
        var authResult = this.userAuth.validateSessionToken(sessionToken);
        if (authResult.isError())
            return Result.err(authResult.error());

        var validateResult = UserService.validateUpdateUserOps(ops);
        if (validateResult.isError())
            return Result.err(validateResult.error());

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
        if (result.isError())
            return Result.err(result.error());
        if (result.isOk()) // TODO: double check this
            uploadAuctionMedia(ops.getImage());

        this.cache.setUser(result.value());

        return Result.ok();
    }

    /**
     * "Logs" in a user with given identifier and given password
     * The user will be authenticated if the password stored in the application's
     * user database
     * matches the one given from the request and is given a sessionToken through a
     * cookie with an expiration
     * time of 30 minutes
     * 
     * @param userId   identifier of the user
     * @param password password of the user
     * @return 200 with generated session token,
     *         404 if the user does not exist
     *         403 if the passwords do not match
     */
    @Override
    public Result<String, ServiceError> authenticateUser(String userId, String password) {
        return this.userAuth.authenticate(userId, password);
    }

}
