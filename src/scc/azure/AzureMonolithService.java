package scc.azure;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosPatchOperations;

import jakarta.ws.rs.core.Cookie;
import scc.cache.*;
import scc.azure.config.AzureMonolithConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;
import scc.cache.redis.*;
import scc.services.AuctionService;
import scc.services.MediaService;
import scc.services.ServiceError;
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
    private final UserAuth userAuth;

    //caches
    private final AuctionCache auctionCache;
    private final BidCache bidCache;
    private final MediaCache mediaCache;
    private final QuestionCache questionCache;
    private final UserCache userCache;

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
        if (config.isCachingEnabled()) {
            this.auctionCache = new RedisAuctionCache(redisConfig);
            this.bidCache = new RedisBidCache(redisConfig);
            this.mediaCache = new RedisMediaCache(redisConfig);
            this.questionCache = new RedisQuestionCache(redisConfig);
            this.userCache = new RedisUserCache(redisConfig);
        } else {
            this.auctionCache = new NoOpCache();
            this.bidCache = new NoOpCache();
            this.mediaCache = new NoOpCache();
            this.questionCache = new NoOpCache();
            this.userCache = new NoOpCache();
        }
        this.userAuth = new UserAuth(this.userDB, RedisCache.getInstance(config.getRedisConfig()));
    }

    /**
     * Creates an auction for a user with given identifier.
     * The user creating the auction must be logged in to execute this operation.
     * @param params JSON that contains the necessary information to create an
     *               auction
     * @param auth Cookie related to the user being "logged" in the application
     * @return 200 with auction's identifier if successful,
     * 404 if the user does not exist
     * 403 if the authentication phase failed
     */
    @Override
    public Result<String, ServiceError> createAuction(CreateAuctionParams params, Cookie auth) {
        var validateResult = AuctionService.validateCreateAuctionParams(params);
        if (validateResult.isError())
            return Result.err(validateResult.error());

        var authResult = this.userAuth.validateSessionToken(auth.getValue());
        if (authResult.isError())
            return Result.err(authResult.error());

        if (!this.userDB.userExists(params.userId()))
            return Result.err(ServiceError.USER_NOT_FOUND);

        var pictureId = params.image().map(mediaStorage::createAuctionMediaID);
        var auctionDao = new AuctionDAO(params.title(), params.description(), pictureId.orElse(null), params.userId(),
                new Date(), params.initialPrice());
        var response = this.auctionDB.createAuction(auctionDao);

        if (response.isOk() && params.image().isPresent())
            uploadAuctionMedia(params.image().get());

        // Store in cache
        auctionCache.set(auctionDao.getId(), auctionDao.toString());
        // TODO Delete search entries from cache
        return response.map(AuctionDAO::getId);
    }

    /**
     * Sets the auction state into deleted so, when displayed to the user, it will appear "created by DELETED USER".
     * This operation does not need authentication as the DeleteUser already did it.
     * @param auctionId identifier of the auction to be deleted
     * @return 204 if deleted successfully,
     * 404 if the auction doesn't exist (do we really need to check this?)
     */
    @Override
    public Result<Void, ServiceError> deleteAuction(String auctionId) {
        var result = this.auctionDB.deleteAuction(auctionId);
        // Delete from cache
        if (result.isOk())
            auctionCache.deleteAuction(auctionId);
        // TODO Delete search entries from cache
        return this.auctionDB.deleteAuction(auctionId);
    }

    /**
     * Lists all auctions created by a user with given identifier.
     * This operation does not need authentication as
     * any user should be able to see the auctions from any other user, even their own.
     * @param userId identifier of the user
     * @return 200 with all user's auctions or empty,
     * 404 if the user does not exist
     */
    @Override
    public Result<List<String>, ServiceError> listAuctionsOfUser(String userId) {
        // TODO Get search result from cache if it exists
        // TODO If not, make the search in database and save in cache
        return this.auctionDB.listAuctionsOfUser(userId)
                .map(auctions -> auctions.stream().map(AuctionDAO::getId).collect(Collectors.toList()));
    }

    /**
     * Updates all the auction's values into new given ones.
     * The user who is the owner of the auction must be logged in order to execute this operation.
     * @param auctionId identifier of the auction to be updated
     * @param ops Values that are to be changed to new ones
     * @param auth Cookie related to the user being "logged" in the application
     * @return 204 if successful on changing the auction's values,
     * 404 if the auction does not exist,
     * 403 if the authentication phase failed
     */
    @Override
    public Result<Void, ServiceError> updateAuction(String auctionId, UpdateAuctionOps ops, Cookie auth) {
        var authResult = this.userAuth.validateSessionToken(auth.getValue());
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
            // Store in cache the updated value
            auctionCache.set(updatedAuction.getId(), updatedAuction.toString());
            uploadAuctionMedia(ops.getImage());
            // TODO Delete searches entries from cache
            // TODO Searching for lists in cache that contain this auction might be exhausting
        }

        return result;
    }

    /**
     * Creates a bid in an auction with given identifier from the logged user. If the bid overpowers all current ones,
     * the auction's winning bid will be set to this one's identifier
     * The user making the bid must be logged in to execute this operation
     * @param params Parameters required to create a bid object
     * @param auth Cookie related to the user being "logged" in the application
     * @return 200 with bid's generated identifier,
     * 404 if the user making the bid or the auction doesn't exist
     * 403 if the authentication phase failed
     */
    @Override
    public Result<String, ServiceError> createBid(CreateBidParams params, Cookie auth) {
        var authResult = this.userAuth.validateSessionToken(auth.getValue());
        if (authResult.isError())
            return Result.err(authResult.error());

        if (!this.userDB.userExists(params.userId()))
            return Result.err(ServiceError.USER_NOT_FOUND);

        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        var bidDao = new BidDAO(params.auctionId(), params.userId(), params.price());
        var response = this.bidDB.createBid(bidDao);

        // Store in cache
        bidCache.set(bidDao.getId(), bidDao.toString());
        // TODO Delete search entries from cache
        return response.map(BidDAO::getId);
    }

    /**
     * Lists all bids made on an auction with given identifier.
     * This operation does not need authentication as any user should be
     * able to see any auction's bids to know whether they bid on it or not.
     * @param auctionId identifier of the auction
     * @return 200 with list of bids in the auction or empty,
     * 404 if auction doesn't exist
     */
    @Override
    public Result<List<BidItem>, ServiceError> listBids(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        // TODO Get search result from cache if it exists
        // TODO If not, make the search in database and save in cache

        return this.bidDB.listBids(auctionId).stream()
                .map(bid -> new BidItem(bid.getId(), bid.getUserId(), bid.getAmount()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Result::ok));
    }

    /**
     * Creates a question on an auction with given identifier made by a user with given identifier
     * The user making the question must be logged in to make this operation
     * @param params Parameters required to create a question
     * @param auth Cookie related to the user being "logged" in the application
     * @return 200 with generated question's identifier,
     * 404 if the user or the auction does not exist,
     * 403 if the authentication phase failed
     */
    @Override
    public Result<String, ServiceError> createQuestion(CreateQuestionParams params, Cookie auth) {
        var authResult = this.userAuth.validateSessionToken(auth.getValue());
        if (authResult.isError())
            return Result.err(authResult.error());

        if (!this.userDB.userExists(params.userId()))
            return Result.err(ServiceError.USER_NOT_FOUND);
        if (!this.auctionDB.auctionExists(params.auctionId()))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        var questionDao = new QuestionDAO(params.auctionId(), params.userId(), params.question());
        var response = this.questionDB.createQuestion(questionDao);

        // Store in cache
        questionCache.set(questionDao.getId(), questionDao.toString());
        // TODO Delete search entries from cache
        return response.map(QuestionDAO::getId);
    }

    /**
     * Creates a reply destined towards a specific question made in a given auction that is answered by the owner
     * The user who is owner of the auction must be logged in to execute this operation
     * @param params Parameters required to create a reply
     * @param auth Cookie related to the user being "logged" in the application
     * @return 200 with generated reply's identifier,
     * 404 if the user or auction does not exist,
     * 403 if the authentication phase failed
     */
    @Override
    public Result<Void, ServiceError> createReply(CreateReplyParams params, Cookie auth) {
        var authResult = this.userAuth.validateSessionToken(auth.getValue());
        if (authResult.isError())
            return Result.err(authResult.error());

        if (!this.userDB.userExists(params.userId()))
            return Result.err(ServiceError.USER_NOT_FOUND);

        var response = this.questionDB.createReply(params.questionId(),
                new QuestionDAO.Reply(params.userId(), params.reply()));

        if (response.isError())
            return Result.err(response.error());

        var repliedQuestion = response.value();
        // Store in cache
        questionCache.set(repliedQuestion.getId(), repliedQuestion.toString());
        // TODO Delete search entries from cache
        return Result.ok();
    }

    /**
     * Lists all the questions made in an auction with given identifier
     * @param auctionId identifier of the auction
     * @return 200 with the questions made in the auction or empty,
     * 404 if the auction does not exist
     */
    @Override
    public Result<List<QuestionItem>, ServiceError> listQuestions(String auctionId) {
        if (!this.auctionDB.auctionExists(auctionId))
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        // TODO Get search result from cache if it exists
        // TODO If not, make the search in database and save in cache
        return this.questionDB.listQuestions(auctionId).stream()
                .map(question -> new QuestionItem(question.getId(), question.getUserId(),
                        question.getQuestion(),
                        Optional.ofNullable(question.getReply())
                                .map(reply -> new ReplyItem(reply.getUserId(), reply.getReply()))))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Result::ok));
    }

    /**
     * Uploads a media resource into the blob storage
     * @param contents bytes of the media resource
     * @return Uploaded media resource's generated identifier
     */
    @Override
    public String uploadAuctionMedia(byte[] contents) {
        var res = this.mediaStorage.uploadAuctionMedia(contents);
        // Store bytes in cache
        mediaCache.setAuctionMedia(Hash.of(contents), contents);
        return res;
    }

    /**
     * Uploads a media resource destined for a user with given identifier
     * @param userId identifier of the user
     * @param contents bytes of the media resource
     * @return Uploaded media resource's generated identifier
     */
    @Override
    public String uploadUserProfilePicture(String userId, byte[] contents) {
        var res = this.mediaStorage.uploadUserMedia(contents);
        // Store bytes in cache
        mediaCache.setUserMedia(Hash.of(contents), contents);
        return res;
    }

    /**
     * Downloads the contents of a media resource with given identifier stored in the blob storage
     * @param id identifier of the media resource
     * @return bytes of the associated media resource
     */
    @Override
    public Optional<byte[]> downloadMedia(String id) {
        // Get media from cache if it exists
        // TODO distinguish auction from user i'm out of time now :(
        var cacheRes = this.mediaCache.getBytes(id);
        if(cacheRes.isPresent()) return cacheRes;
        return this.mediaStorage.downloadMedia(id);
    }

    /**
     * Deletes the contents of a media resource with given identifier from the blob storage
     * @param id identifier of the media resource
     * @return true if it was deleted, false otherwise
     */
    @Override
    public boolean deleteMedia(String id) {
        var res = this.mediaStorage.deleteMedia(id);
        if(res)
            // TODO distinguish auction from user i'm out of time now :(
            mediaCache.deleteMedia(id); // delete from cache
        return res;
    }

    /**
     * Creates a user with given values to be stored in the application's user database.
     * This operation does not need authentication as anyone outside the application should be able to register
     * to the application.
     * @param params Parameters required to create a user object
     * @return 200 with generated user's identifier,
     * 409 if the user with given identifier already exists
     */
    @Override
    public Result<String, ServiceError> createUser(CreateUserParams params) {
        var validateResult = UserService.validateCreateUserParams(params);
        if (validateResult.isError())
            return Result.err(validateResult.error());

        var photoId = params.image().map(img -> this.mediaStorage.createUserMediaID(img));
        var userDao = new UserDAO(params.nickname(), params.name(), Hash.of(params.password()), photoId.orElse(null));
        var result = this.userDB.createUser(userDao);
        if (result.isOk() && params.image().isPresent())
            uploadAuctionMedia(params.image().get());

        var createdUser = result.value();

        // Store in cache
        userCache.set(createdUser.getId(), createdUser.toString());

        return result.map(UserDAO::getId);
    }

    /**
     * Deletes a user with given identifier from the application's user database.
     * The user logged in must be the same as the one from the request and, after the operation is executed,
     * we make the cookie invalid.
     * All the auctions associated with this user must be in the state of deleted as in, when
     * performing reads on any of them, we show that it was created by a "DELETED USER".
     * @param userId identifier of the user
     * @param auth Cookie related to the user being "logged" in the application
     * @return 204 if deleted successfully,
     * 403 if the authentication phase failed
     */
    @Override
    public Result<Void, ServiceError> deleteUser(String userId, Cookie auth) {
        var authResult = this.userAuth.validateSessionToken(auth.getValue());
        if (authResult.isError())
            return Result.err(authResult.error());

        var result = this.userDB.deleteUser(userId);
        if (!result.isOk())
            return Result.err(result.error());

        String photoId = result.value().getPhotoId();
        if (!userDB.userWithPhoto(photoId))
            deleteMedia(photoId);

        auctionDB.deleteUserAuctions(userId);

        questionDB.deleteQuestionFromUser(userId);
        bidDB.deleteBidsFromUser(userId);
        userAuth.deleteSessionToken(auth.getValue());

        // Delete from cache
        userCache.deleteUser(userId);
        // TODO Delete search entries from cache
        return Result.ok();
    }

    /**
     * Updates the values stored in a user with given identifier to new ones
     * The updated user must be logged in to execute this operation
     * @param userId identifier of the user
     * @param ops Values to be placed on the user
     * @param auth Cookie related to the user being "logged" in the application
     * @return 204 if updated successfully,
     * 404 if the user does not exist,
     * 403 if the authentication phase failed
     * 400 if the request is not well-formed
     */
    @Override
    public Result<Void, ServiceError> updateUser(String userId, UpdateUserOps ops, Cookie auth) {
        var authResult = this.userAuth.validateSessionToken(auth.getValue());
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
        if (result.isOk())
            uploadAuctionMedia(ops.getImage());

        var updatedUser = result.value();
        // Store in cache
        userCache.set(updatedUser.getId(), updatedUser.toString());

        return Result.ok();
    }

    /**
     * "Logs" in a user with given identifier and given password
     * The user will be authenticated if the password stored in the application's user database
     * matches the one given from the request and is given a sessionToken through a cookie with an expiration
     * time of 30 minutes
     * @param userId identifier of the user
     * @param password password of the user
     * @return 200 with generated session token,
     * 404 if the user does not exist
     * 403 if the passwords do not match
     */
    @Override
    public Result<String, ServiceError> authenticateUser(String userId, String password) {
        return this.userAuth.authenticate(userId, password);
    }

}
