package scc.kube;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.ConnectionString;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import scc.Result;
import scc.ServiceError;
import scc.kube.config.MongoConfig;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.QuestionDao;
import scc.kube.dao.UserDao;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

public class Mongo {
    private static final Logger logger = Logger.getLogger(Mongo.class.getName());

    private final MongoClient client;
    private final MongoCollection<AuctionDao> auctionCollection;
    private final MongoCollection<BidDao> bidCollection;
    private final MongoCollection<QuestionDao> questionCollection;
    private final MongoCollection<UserDao> userCollection;

    public Mongo(MongoConfig config) {
        var pojoCodecRegistry = CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
        var codecRegistry = CodecRegistries.fromRegistries(
                com.mongodb.MongoClientSettings.getDefaultCodecRegistry(),
                pojoCodecRegistry);
        var clientSettings = com.mongodb.MongoClientSettings.builder()
                .codecRegistry(codecRegistry)
                .applyConnectionString(new ConnectionString(config.connectionUri))
                .build();
        var client = MongoClients.create(clientSettings);
        var database = client.getDatabase(config.databaseName);

        this.client = client;
        this.auctionCollection = database.getCollection(config.auctionCollection, AuctionDao.class);
        this.bidCollection = database.getCollection(config.bidCollection, BidDao.class);
        this.questionCollection = database.getCollection(config.questionCollection, QuestionDao.class);
        this.userCollection = database.getCollection(config.userCollection, UserDao.class);

        this.auctionCollection.createIndex(Indexes.hashed("user_id"));
        this.auctionCollection.createIndex(Indexes.descending("close_time"));

        this.bidCollection.createIndex(Indexes.hashed("auction_id"));

        this.questionCollection.createIndex(Indexes.hashed("auction_id"));

        this.userCollection.createIndex(Indexes.text("user_id"), new IndexOptions().unique(true));
    }

    public void close() {
        this.client.close();
    }

    /* ------------------------- Auction ------------------------- */

    /**
     * Get an auction by its ID.
     * 
     * @param auctionId The ID of the auction.
     * @return the auction, or null if it doesn't exist
     */
    public AuctionDao getAuction(ObjectId auctionId) {
        var auction = this.auctionCollection.find(Filters.eq("_id", auctionId)).first();
        logger.fine("getAuction: " + auction);
        return auction;
    }

    /**
     * Create a new auction.
     * 
     * @param auction The auction to create.
     * @return the auction, with its ID set
     */
    public AuctionDao createAuction(AuctionDao auction) {
        this.auctionCollection.insertOne(auction);
        logger.fine("createAuction: " + auction);
        return auction;
    }

    /**
     * Updates the auction with the given `auctionId`.
     * The updateable fields are: `title`, `description`, `imageId`.
     * 
     * @param auctionId  The ID of the auction to update
     * @param auctionDao The auction dao to update with
     * @return the updated auction
     */
    public AuctionDao updateAuction(ObjectId auctionId, AuctionDao auctionDao) {
        var updates = new ArrayList<Bson>();

        if (auctionDao.getTitle() != null)
            updates.add(Updates.set("title", auctionDao.getTitle()));
        if (auctionDao.getDescription() != null)
            updates.add(Updates.set("description", auctionDao.getDescription()));
        if (auctionDao.getImageId() != null)
            updates.add(Updates.set("image_id", auctionDao.getImageId()));

        var updated = this.auctionCollection.findOneAndUpdate(Filters.eq("_id", auctionId), updates);
        logger.fine("updateAuction: " + updated);
        return updated;
    }

    /**
     * Updates the highest bid of an auction.
     * The provided `bid` must have a higher value than the current highest bid.
     * If the update succeeds, the auction was updated and should be invalided from
     * cache. The provided `auction` object is not updated.
     * 
     * @param auction The auction to update.
     * @param bid     The new highest bid.
     * @return The updated auction, or null if the update failed.
     */
    public Result<AuctionDao, ServiceError> updateHighestBid(AuctionDao auction, BidDao bid) {
        var highestBidId = auction.getHighestBid();
        while (true) {
            var result = this.auctionCollection.findOneAndUpdate(
                    Filters.and(
                            Filters.eq("_id", auction.getId()),
                            Filters.eq("status", "OPEN"),
                            Filters.or(Filters.not(Filters.exists("highest_bid")),
                                    Filters.eq("highest_bid", highestBidId))),
                    Updates.set("highest_bid", bid.getId()));

            if (result != null)
                return Result.ok(result);

            auction = this.getAuction(auction.getId());
            if (auction == null)
                return Result.err(ServiceError.AUCTION_NOT_FOUND);
            if (!auction.getStatus().equals(AuctionDao.Status.OPEN))
                return Result.err(ServiceError.AUCTION_NOT_OPEN);
            var highestBid = this.getBid(auction.getHighestBid());
            if (highestBid == null)
                throw new IllegalStateException("Auction has no highest bid");
            if (highestBid.getValue() >= bid.getValue())
                return Result.err(ServiceError.BID_CONFLICT);
            highestBidId = highestBid.getId();
        }
    }

    public Result<AuctionDao, ServiceError> closeAuction(ObjectId auctionId) {
        logger.fine("closeAuction: trying to close" + auctionId);
        var updated = this.auctionCollection.findOneAndUpdate(Filters.eq("_id", auctionId),
                Updates.set("state", AuctionDao.Status.CLOSED));
        if (updated == null)
            return Result.err(ServiceError.AUCTION_NOT_FOUND);
        logger.fine("closeAuction: closed " + updated);
        return Result.ok(updated);
    }

    public List<AuctionDao> listAuctionsSoonToClose(int skip, int limit) {
        var future = LocalDateTime.now().plus(Duration.ofSeconds(300));
        var iter = this.auctionCollection.find(
                Filters.and(
                        Filters.eq("status", "OPEN"),
                        Filters.lt("close_time", future)))
                .skip(skip).limit(limit);
        var auctions = new ArrayList<AuctionDao>();
        iter.iterator().forEachRemaining(auctions::add);
        logger.fine("listAuctionsSoonToClose: " + auctions);
        return auctions;
    }

    /* ------------------------- Bid ------------------------- */

    /**
     * Get a bid by its ID.
     * 
     * @param bidId The ID of the bid.
     * @return the bid, or null if it doesn't exist
     */
    public BidDao getBid(ObjectId bidId) {
        var bid = this.bidCollection.find(Filters.eq("_id", bidId)).first();
        logger.fine("getBid: " + bid);
        return bid;
    }

    public HashMap<ObjectId, BidDao> getBidMany(List<ObjectId> bidIds) {
        var bids = this.bidCollection.find(Filters.in("_id", bidIds));
        var result = new HashMap<ObjectId, BidDao>();
        bids.iterator().forEachRemaining(bid -> result.put(bid.getId(), bid));
        logger.fine("getBidMany: " + result);
        return result;
    }

    /**
     * Create a new bid.
     * 
     * @param bid The bid to create.
     * @return the bid, with its ID set
     */
    public BidDao createBid(BidDao bid) {
        this.bidCollection.insertOne(bid);
        logger.fine("createBid: " + bid);
        return bid;
    }

    /**
     * Lists bids for an auction.
     * 
     * @param auctionId The ID of the auction.
     * @param skip      The number of bids to skip.
     * @param limit     The maximum number of bids to return.
     * @return the list of bids
     */
    public List<BidDao> listAuctionBids(ObjectId auctionId, int skip, int limit) {
        var iter = this.bidCollection.find(
                Filters.eq("auction_id", auctionId))
                .skip(skip).limit(limit).sort(Sorts.descending("value"));
        var bids = new ArrayList<BidDao>();
        iter.iterator().forEachRemaining(bids::add);
        logger.fine("listAuctionBids: " + bids);
        return bids;
    }

    /* ------------------------- Question ------------------------- */

    /**
     * Get a question by its ID.
     * 
     * @param questionId The ID of the question.
     * @return the question, or null if it doesn't exist
     */
    public QuestionDao getQuestion(ObjectId questionId) {
        var question = this.questionCollection.find(Filters.eq("_id", questionId)).first();
        logger.fine("getQuestion: " + question);
        return question;
    }

    /**
     * Create a new question.
     * 
     * @param question The question to create.
     * @return the question, with its ID set
     */
    public QuestionDao createQuestion(QuestionDao question) {
        this.questionCollection.insertOne(question);
        logger.fine("createQuestion: " + question);
        return question;
    }

    /**
     * Create reply to a question.
     * 
     * @param questionId The ID of the question to reply to.
     * @param reply      The reply to create.
     * @return the updated question
     */
    public Result<QuestionDao, ServiceError> createReply(ObjectId questionId, String reply) {
        logger.fine("createReply: attempting to reply to " + questionId + " with " + reply);
        var question = this.getQuestion(questionId);
        if (question == null) {
            logger.fine("createReply: question not found");
            return Result.err(ServiceError.QUESTION_NOT_FOUND);
        }

        var updated = this.questionCollection.findOneAndUpdate(
                Filters.and(Filters.eq("_id", questionId), Filters.eq("reply", null)),
                Updates.set("reply", reply));

        if (updated == null) {
            logger.fine("createReply: question already replied to");
            return Result.err(ServiceError.QUESTION_ALREADY_REPLIED);
        }

        logger.fine("createReply: replied to " + questionId);
        return Result.ok(updated);
    }

    public List<QuestionDao> listAuctionQuestions(ObjectId auctionId, int skip, int limit) {
        var iter = this.questionCollection.find(
                Filters.eq("auction_id", auctionId))
                .skip(skip).limit(limit);
        var questions = new ArrayList<QuestionDao>();
        iter.iterator().forEachRemaining(questions::add);
        logger.fine("listAuctionQuestions: " + questions);
        return questions;
    }

    /* ------------------------- User ------------------------- */

    public UserDao getUser(String userId) {
        var user = this.userCollection.find(Filters.eq("user_id", userId)).first();
        logger.fine("getUser: " + user);
        return user;
    }

    public HashMap<String, UserDao> getUserMany(List<String> userIds) {
        var users = this.userCollection.find(Filters.in("user_id", userIds));
        var map = new HashMap<String, UserDao>();
        for (var user : users)
            map.put(user.getUserId(), user);
        logger.fine("getUserMany: " + users);
        return map;
    }

    public Result<UserDao, ServiceError> createUser(UserDao user) {
        try {
            this.userCollection.insertOne(user);
            logger.fine("createUser: " + user);
            return Result.ok(user);
        } catch (MongoWriteException e) {
            logger.fine("createUser: user already exists");
            logger.fine(e.toString());
            return Result.err(ServiceError.USER_ALREADY_EXISTS);
        }
    }

    public Result<UserDao, ServiceError> updateUser(UserDao userDao) {
        var updates = new ArrayList<Bson>();

        if (userDao.getName() != null)
            updates.add(Updates.set("name", userDao.getName()));
        if (userDao.getHashedPassword() != null)
            updates.add(Updates.set("hashed_password", userDao.getHashedPassword()));
        // if (userDao.getImageId() != null) // TODO: fix this
        // updates.add(Updates.set("image_id", auctionDao.getImageId()));

        var updated = this.userCollection.findOneAndUpdate(Filters.eq("user_id", userDao.getUserId()), updates);
        logger.fine("updateUser: " + updated);
        return Result.ok(updated);
    }

    public Result<UserDao, ServiceError> deactivateUser(ObjectId userId) {
        logger.fine("deactivateUser: attempting to deactivate " + userId);
        var updated = this.userCollection.findOneAndUpdate(
                Filters.eq("_id", userId),
                Updates.set("status", UserDao.Status.INACTIVE));
        if (updated == null) {
            logger.fine("deactivateUser: user not found or already inactive");
            return Result.err(ServiceError.USER_NOT_FOUND);
        }
        logger.fine("deactivateUser: deactivated " + userId);
        return Result.ok(updated);
    }
}
