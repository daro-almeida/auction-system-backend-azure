package scc.azure;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.Result;
import scc.ServiceError;
import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;

public class Cosmos {

    public static CosmosDatabase createDatabase(CosmosDbConfig config) {
        var cosmosClient = new CosmosClientBuilder()
                .endpoint(config.dbUrl)
                .key(config.dbKey)
                .directMode()
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        var dbClient = cosmosClient.getDatabase(config.dbName);
        return dbClient;
    }

    /* ------------------------- Auction External ------------------------- */

    /**
     * Gets the auction saved in the database with given identifier
     * 
     * @param container auctions container
     * @param auctionId identifier of the auction
     * @return Object that represents the auction
     */
    public static Optional<AuctionDAO> getAuction(CosmosContainer container, String auctionId) {
        var options = createAuctionQueryOptions(auctionId);
        return container
                .queryItems(
                        "SELECT * FROM auctions WHERE auctions.id=\"" + auctionId + "\"",
                        options,
                        AuctionDAO.class)
                .stream()
                .findFirst();
    }

    /**
     * Checks if the database contains an auction with given identifier
     * 
     * @param container auctions container
     * @param auctionId identifier of the auction
     * @return true if exists, false otherwise
     */
    public static boolean auctionExists(CosmosContainer container, String auctionId) {
        return getAuction(container, auctionId).isPresent();
    }

    /**
     * Creates an entry in the database that represents an auction
     * 
     * @param container auctions container
     * @param auction   Object that represents an auction
     * @return Ok with new auction's generated identifier or Error
     */
    public static Result<AuctionDAO, ServiceError> createAuction(CosmosContainer container, AuctionDAO auction) {
        if (auction.getId() == null)
            auction.setId(UUID.randomUUID().toString());
        var response = container.createItem(auction);
        return Result.ok(response.getItem());
    }

    /**
     * Updates the values in the database for an auction with given identifier
     * 
     * @param container auctions container
     * @param auctionId identifier of the auction
     * @param auction   dao with fields to update
     * @return Ok if updated, Error if not
     */
    public static Result<AuctionDAO, ServiceError> updateAuction(
            CosmosContainer container,
            String auctionId,
            AuctionDAO auction) {
        var partitionKey = createAuctionPartitionKey(auctionId);
        try {
            var ops = CosmosPatchOperations.create();

            if (auction.getTitle() != null)
                ops.set("/title", auction.getTitle());
            if (auction.getDescription() != null)
                ops.set("/description", auction.getDescription());
            if (auction.getPictureId() != null)
                ops.set("/pictureId", auction.getPictureId());

            return Result.ok(container.patchItem(auctionId, partitionKey, ops, AuctionDAO.class).getItem());
        } catch (CosmosException e) {
            return Result.err(ServiceError.AUCTION_NOT_FOUND);
        }
    }

    public static Result<List<AuctionDAO>, ServiceError> listAuctionsOfUser(
            CosmosContainer container,
            String userId,
            boolean open) {
        var options = createAuctionQueryOptions();
        if (open) {
            var auctions = container
                    .queryItems(
                            "SELECT * FROM auctions WHERE auctions.userId=\"" + userId
                                    + "\" and auctions.status=\"OPEN\"",
                            options,
                            AuctionDAO.class)
                    .stream().toList();
            return Result.ok(auctions);
        } else {
            var auctions = container
                    .queryItems(
                            "SELECT * FROM auctions WHERE auctions.userId=\"" + userId + "\"",
                            options,
                            AuctionDAO.class)
                    .stream().toList();
            return Result.ok(auctions);
        }
    }

    /**
     * Finds the auctions that the user has bid on
     * 
     * @param auctionContainer auctions container
     * @param bidContainer     bids container
     * @param userId           identifier of the user
     * @return List of auction identifiers
     */
    public static Result<List<String>, ServiceError> listAuctionsFollowedByUser(
            CosmosContainer auctionsContainer,
            CosmosContainer bidsContainer,
            String userId) {
        return null;
    }

    public static Result<List<AuctionDAO>, ServiceError> listAuctionsAboutToClose(CosmosContainer container) {
        var WHAT_IS_RECENT = 60 * 5;
        // TODO this probably can be optimized
        var auctions = container
                .queryItems(
                        "SELECT * FROM auctions",
                        new CosmosQueryRequestOptions(),
                        AuctionDAO.class);
        var auctionsAboutToClose = new ArrayList<AuctionDAO>();
        for (AuctionDAO dao : auctions.stream().toList()) {
            long diffInMilliesButItsActuallySecs = Math.abs(ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() -
                    dao.getEndTime().toEpochSecond(ZoneOffset.UTC));
            if (diffInMilliesButItsActuallySecs < WHAT_IS_RECENT)
                auctionsAboutToClose.add(dao);
        }
        return Result.ok(auctionsAboutToClose);
    }

    /* ------------------------- Bid External ------------------------- */

    /**
     * Returns the bid saved in the database with given identifier
     * 
     * @param container bids container
     * @param bidId     identifier of the bid
     * @return Object that represents a bid
     */
    public static Optional<BidDAO> getBid(CosmosContainer container, String bidId) {
        var options = createBidQueryOptions(bidId);
        return container
                .queryItems(
                        "SELECT * FROM bids WHERE bids.id=\"" + bidId + "\"",
                        options,
                        BidDAO.class)
                .stream()
                .findFirst();
    }

    /**
     * Checks if a bid with given identifier exists in the database
     * 
     * @param container bids container
     * @param bidId     identifier of the bid
     * @return True if exists in the database, false otherwise
     */
    public static boolean bidExists(CosmosContainer container, String bidId) {
        return getBid(container, bidId).isPresent();
    }

    /**
     * Creates an entry in the database that represents a bid
     * 
     * @param container bids container
     * @param bid       Object that represents a bid
     * @return Ok with new bid or Error
     */
    public static Result<BidDAO, ServiceError> createBid(CosmosContainer container, BidDAO bid) {
        if (bid.getId() == null)
            bid.setId(UUID.randomUUID().toString());
        var response = container.createItem(bid);
        return Result.ok(response.getItem());
    }

    /**
     * Lists all the bids present in an auction with a given identifier
     * 
     * @param container bids container
     * @param auctionId identifier of the auction
     * @return List of bids made in the auction
     */
    public static Result<List<BidDAO>, ServiceError> listBidsOfAuction(CosmosContainer container, String auctionId) {
        var options = createBidQueryOptions(auctionId);
        return Result.ok(container
                .queryItems(
                        "SELECT * FROM bids WHERE bids.auctionId=\"" + auctionId + "\"",
                        options,
                        BidDAO.class)
                .stream().collect(Collectors.toList()));
    }

    /* ------------------------- Question External ------------------------- */

    /**
     * Returns the question present in the database with given identifier
     * 
     * @param container  questions container
     * @param questionId identifier of the question
     * @return Object that represents the question
     */
    public static Optional<QuestionDAO> getQuestion(CosmosContainer container, String questionId) {
        var options = createQuestionQueryOptions(questionId);
        return container
                .queryItems(
                        "SELECT * FROM questions WHERE questions.id=\"" + questionId + "\"",
                        options,
                        QuestionDAO.class)
                .stream()
                .findFirst();
    }

    /**
     * Checks if the question with given identifier exists in the database
     * 
     * @param container  questions container
     * @param questionId identifier of the question
     * @return true if it exists, false otherwise
     */
    public static boolean questionExists(CosmosContainer container, String questionId) {
        return getQuestion(container, questionId).isPresent();
    }

    /**
     * Inserts an entry in the database that represents a question in an auction
     * 
     * @param container questions container
     * @param question  Object that represents a question
     * @return 200 with question's identifier
     */
    public static Result<QuestionDAO, ServiceError> createQuestion(CosmosContainer container, QuestionDAO question) {
        if (question.getId() == null)
            question.setId(UUID.randomUUID().toString());
        var response = container.createItem(question);
        return Result.ok(response.getItem());
    }

    /**
     * Updates the question with a reply to it in the database
     * 
     * @param container  questions container
     * @param questionId identifier of the question
     * @param reply      Object that represents a reply to the question
     * @return 200 with reply's identifier
     */
    public static Result<QuestionDAO, ServiceError> createReply(
            CosmosContainer container,
            String questionId,
            QuestionDAO.Reply reply) {
        var question = getQuestion(container, questionId);
        if (question.isEmpty())
            return Result.err(ServiceError.QUESTION_NOT_FOUND);
        if (question.get().getReply() != null)
            return Result.err(ServiceError.QUESTION_ALREADY_REPLIED);

        var partitionKey = createQuestionPartitionKey(questionId);
        var patch = CosmosPatchOperations.create();
        patch.add("/reply", reply);
        var response = container.patchItem(questionId, partitionKey, patch, QuestionDAO.class);

        return Result.ok(response.getItem());
    }

    /**
     * Gathers all questions that are saved in an auction with given identifier
     * 
     * @param container questions container
     * @param auctionId identifier of the auction
     * @return List of questions present in the auction
     */
    public static Result<List<QuestionDAO>, ServiceError> listQuestionsOfAuction(
            CosmosContainer container,
            String auctionId) {
        var options = createQuestionQueryOptions(auctionId);
        var questions = container
                .queryItems(
                        "SELECT * FROM questions WHERE questions.auctionId=\"" + auctionId + "\"",
                        options,
                        QuestionDAO.class)
                .stream().collect(Collectors.toList());
        return Result.ok(questions);
    }

    /* ------------------------- User External ------------------------- */

    /**
     * Returns the user with given identifier from the database
     * 
     * @param container users container
     * @param userId    Identifier of the user requested
     * @return Object which represents the user in the database
     */
    public static Optional<UserDAO> getUser(CosmosContainer container, String userId) {
        var options = createUserQueryOptions(userId);
        return container
                .queryItems(
                        "SELECT * FROM users WHERE users.id=\"" + userId + "\"",
                        options,
                        UserDAO.class)
                .stream()
                .findFirst();
    }

    /**
     * Checks if a user with given identifier exists in the database
     * 
     * @param container users container
     * @param userId    Identifier of the user requested
     * @return True if exists in the database, false otherwise
     */
    public static boolean userExists(CosmosContainer container, String userId) {
        return getUser(container, userId).isPresent();
    }

    /**
     * Saves the user in object form into the database
     * 
     * @param container users container
     * @param user      Object that represents the user
     * @return 200 with created user's nickname or error if it already exists in the
     *         database
     */
    public static Result<UserDAO, ServiceError> createUser(CosmosContainer container, UserDAO user) {
        try {
            var response = container.createItem(user);
            return Result.ok(response.getItem());
        } catch (CosmosException e) {
            return Result.err(ServiceError.USER_ALREADY_EXISTS);
        }
    }

    /**
     * Deletes the user with given nickname from the database
     * 
     * @param container users container
     * @param userId    nickname of the user to be deleted
     * @return 204 if successful, respective error otherwise
     */
    public static Result<UserDAO, ServiceError> deleteUser(CosmosContainer container, String userId) {
        var userOpt = getUser(container, userId);
        if (userOpt.isEmpty())
            return Result.err(ServiceError.USER_NOT_FOUND);
        var user = userOpt.get();

        var options = createUserRequestOptions(userId);
        var partitionKey = createUserPartitionKey(userId);
        try {
            container.deleteItem(userId, partitionKey, options);
            return Result.ok(user);
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(ServiceError.USER_NOT_FOUND);
            throw e;
        }
    }

    /**
     * Updates the values in the user with given nickname with new given values
     * 
     * @param container users container
     * @param userId    nickname of the user to be updated
     * @param user      Object that represents the user with new values
     * @return 200 with updated user, respective error otherwise
     */
    public static Result<UserDAO, ServiceError> updateUser(
            CosmosContainer container,
            String userId,
            UserDAO user) {
        var partitionKey = createUserPartitionKey(userId);
        try {
            var ops = CosmosPatchOperations.create();

            if (user.getName() != null)
                ops.add("/name", user.getName());
            if (user.getHashedPwd() != null)
                ops.add("/hashedPwd", user.getHashedPwd());
            if (user.getPhotoId() != null)
                ops.add("/photoId", user.getPhotoId());

            container.patchItem(userId, partitionKey, ops, UserDAO.class);
            var updatedUser = getUser(container, userId);
            return Result.ok(updatedUser.get());
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(ServiceError.USER_NOT_FOUND);
            throw e;
        }
    }

    /* ------------------------- Mixed External ------------------------- */

    /* ------------------------- Auction Internal ------------------------- */

    private static PartitionKey createAuctionPartitionKey(String auctionId) {
        return new PartitionKey(auctionId);
    }

    private static CosmosItemRequestOptions createAuctionRequestOptions(String auctionId) {
        return new CosmosItemRequestOptions();
    }

    private static CosmosQueryRequestOptions createAuctionQueryOptions() {
        return new CosmosQueryRequestOptions();

    }

    private static CosmosQueryRequestOptions createAuctionQueryOptions(String auctionId) {
        return createAuctionQueryOptions().setPartitionKey(createAuctionPartitionKey(auctionId));
    }

    /* ------------------------- Bid Internal ------------------------- */
    private static PartitionKey createBidPartitionKey(String bidId) {
        return new PartitionKey(bidId);
    }

    private static CosmosQueryRequestOptions createBidQueryOptions(String bidId) {
        var options = new CosmosQueryRequestOptions();
        options.setPartitionKey(createBidPartitionKey(bidId));
        return options;
    }

    private static CosmosItemRequestOptions createBidRequestOptions(String bidId) {
        return new CosmosItemRequestOptions();
    }

    /* ------------------------- Question Internal ------------------------- */

    /**
     * Creates a partition key with given identifier of a question
     * 
     * @param questionId identifier of the question
     * @return PartitionKey with identifier of the question
     */
    private static PartitionKey createQuestionPartitionKey(String questionId) {
        return new PartitionKey(questionId);
    }

    /**
     * Creates a QueryOptions object with a partition key of the identifier of the
     * question
     * 
     * @param questionId identifier of the question
     * @return QueryOptions object with mentioned partition key
     */
    private static CosmosQueryRequestOptions createQuestionQueryOptions(String questionId) {
        var options = new CosmosQueryRequestOptions();
        options.setPartitionKey(createQuestionPartitionKey(questionId));
        return options;
    }

    /* ------------------------- User Internal ------------------------- */

    /**
     * Creates a partition key with given nickaname to be used on database
     * operations
     * 
     * @param userId nickname of the user
     * @return PartitionKey object with user's nickname
     */
    private static PartitionKey createUserPartitionKey(String userId) {
        return new PartitionKey(userId);
    }

    /**
     * Creates an ItemRequestOptions object with given nickname to be used on
     * database operations
     * 
     * @param userId nickname of the user
     * @return CosmosItemRequestOptions object with user's nickname
     */
    private static CosmosItemRequestOptions createUserRequestOptions(String userId) {
        return new CosmosItemRequestOptions();
    }

    /**
     * Creates a QueryRequestOptions object with given nickname to be used on
     * database operations
     * 
     * @param userId nickname of the user
     * @return CosmosQueryRequestOptions object with user's nickname
     */
    private static CosmosQueryRequestOptions createUserQueryOptions(String userId) {
        return new CosmosQueryRequestOptions().setPartitionKey(createUserPartitionKey(userId));
    }
}
