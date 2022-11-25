package scc.kube;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.ConnectionString;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import scc.Result;
import scc.ServiceError;
import scc.kube.config.MongoConfig;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.QuestionDao;
import scc.kube.dao.UserDao;

public class Mongo implements Closeable {
    private static final Logger logger = Logger.getLogger(Mongo.class.getName());

    public final MongoClient client;
    public final MongoDatabase database;
    public final MongoCollection<AuctionDao> auctionCollection;
    public final MongoCollection<QuestionDao> questionCollection;
    public final MongoCollection<UserDao> userCollection;

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
        this.database = database;

        // Auctions
        this.auctionCollection = database.getCollection(config.auctionCollection, AuctionDao.class);
        this.auctionCollection.createIndex(Indexes.hashed("user_id"));
        this.auctionCollection.createIndex(Indexes.descending("create_time"));
        this.auctionCollection.createIndex(Indexes.descending("close_time"));
        this.auctionCollection.createIndex(Indexes.descending("bids._id"));

        // Questions
        this.questionCollection = database.getCollection(config.questionCollection, QuestionDao.class);
        this.questionCollection.createIndex(Indexes.hashed("auction_id"));
        this.questionCollection.createIndex(Indexes.hashed("user_id"));
        this.questionCollection.createIndex(Indexes.descending("create_time"));

        // Users
        this.userCollection = database.getCollection(config.userCollection, UserDao.class);
        this.userCollection.createIndex(Indexes.text("username"), new IndexOptions().unique(true));
    }

    @Override
    public void close() {
        this.client.close();
    }

    /* ------------------------- Auction ------------------------- */

    public Result<AuctionDao, ServiceError> getAuction(ObjectId auctionId) {
        var filter = Filters.eq("_id", auctionId);
        var projection = auctionDaoProjection();
        var auctionDao = this.auctionCollection.find(filter).projection(projection).first();
        if (auctionDao == null)
            return Result.err(ServiceError.AUCTION_NOT_FOUND);
        return Result.ok(auctionDao);
    }

    public Result<HashMap<ObjectId, AuctionDao>, ServiceError> getAuctionMany(List<ObjectId> auctionIds) {
        var filter = Filters.in("_id", auctionIds);
        var projection = auctionDaoProjection();
        var auctionDaos = this.auctionCollection.find(filter).projection(projection).into(new ArrayList<AuctionDao>());
        var auctionMap = new HashMap<ObjectId, AuctionDao>();
        for (var auctionDao : auctionDaos)
            auctionMap.put(auctionDao.id, auctionDao);
        return Result.ok(auctionMap);
    }

    /**
     * Create a new auction.
     * Required fields: title, description, userid,
     * createTime,closeTime,initialprice,status.
     * 
     * @param auctionDao
     * @return
     */
    public AuctionDao createAuction(AuctionDao auctionDao) {
        assert auctionDao.id == null : "Auction ID must be null";
        assert auctionDao.title != null : "Auction title must not be null";
        assert auctionDao.description != null : "Auction description must not be null";
        assert auctionDao.userId != null : "Auction user ID must not be null";
        assert auctionDao.createTime != null : "Auction create time must not be null";
        assert auctionDao.closeTime != null : "Auction close time must not be null";
        assert auctionDao.initialPrice >= 0 : "Auction initial price must be non-negative";
        assert auctionDao.status == AuctionDao.Status.OPEN : "Auction status must be open";

        this.auctionCollection.insertOne(auctionDao);
        assert auctionDao.id != null;

        return auctionDao;
    }

    public Result<AuctionDao, ServiceError> updateAuction(ObjectId auctionId, AuctionDao auctionDao) {
        assert auctionDao.id == null : "Auction ID must be null";
        assert auctionDao.userId == null : "Auction user ID must be null";
        assert auctionDao.userIdDisplay == null : "Auction user ID display must be null";
        assert auctionDao.createTime == null : "Auction create time must be null";
        assert auctionDao.closeTime == null : "Auction close time must be null";
        assert auctionDao.initialPrice == 0 : "Auction initial price must be zero";
        assert auctionDao.status == null : "Auction status must be null";

        var updates = new ArrayList<Bson>();

        if (auctionDao.title != null)
            updates.add(Updates.set("title", auctionDao.title));
        if (auctionDao.description != null)
            updates.add(Updates.set("description", auctionDao.description));
        if (auctionDao.imageId != null)
            updates.add(Updates.set("image_id", auctionDao.imageId));

        var updated = this.auctionCollection.findOneAndUpdate(Filters.eq("_id", auctionId), updates);
        if (updated == null)
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        logger.fine("updateAuction: " + updated);
        return Result.ok(updated);
    }

    public Result<AuctionDao, ServiceError> closeAuction(ObjectId auctionId) {
        logger.fine("closeAuction: trying to close" + auctionId);
        var updated = this.auctionCollection.findOneAndUpdate(Filters.eq("_id", auctionId),
                Updates.set("state", AuctionDao.Status.CLOSED));
        if (updated == null)
            return Result.err(ServiceError.AUCTION_NOT_FOUND);
        updated.status = AuctionDao.Status.CLOSED;
        logger.fine("closeAuction: closed " + updated);
        return Result.ok(updated);
    }

    /* ------------------------- Bid ------------------------- */

    public Result<BidDao, ServiceError> getBid(ObjectId auctionId, ObjectId bidId) {
        var pipeline = Arrays.asList(
                Aggregates.match(Filters.eq("_id", auctionId)),
                Aggregates.unwind("$bids"),
                Aggregates.match(Filters.eq("bids._id", bidId)),
                Aggregates.replaceRoot("$bids"));
        var bidDao = this.auctionCollection.aggregate(pipeline, BidDao.class).first();
        if (bidDao == null)
            return Result.err(ServiceError.BID_NOT_FOUND);
        return Result.ok(bidDao);
    }

    /**
     * Create a new bid.
     * Required fields: userId, userIdDisplay, amount, createTime.
     * 
     * @param auctionId ID of the auction to bid on
     * @param bidDao    Bid to create
     * @return Created bid
     */
    public Result<BidDao, ServiceError> createBid(ObjectId auctionId, BidDao bidDao) {
        assert bidDao.id == null : "Bid ID must be null";
        assert bidDao.userId != null : "Bid user ID must not be null";
        assert bidDao.userIdDisplay != null : "Bid user ID display must not be null";
        assert bidDao.amount >= 0 : "Bid amount must be non-negative";
        assert bidDao.createTime != null : "Bid create time must not be null";

        bidDao.id = new ObjectId();
        var filter = new Document().append("$expr", new Document("$gt", Arrays.asList(
                new Document().append("$getField", new Document("field", "amount").append("input", new Document(
                        "$arrayElemAt", Arrays.asList("$bids", -1)))),
                bidDao.amount)));

        var updated = this.auctionCollection.findOneAndUpdate(filter, Updates.push("bids", bidDao));
        if (updated == null)
            return Result.err(ServiceError.BID_CONFLICT);

        this.userCollection.findOneAndUpdate(
                Filters.eq("_id", bidDao.userId),
                Updates.push("created_bids", bidDao.id));

        // db.auctions.find({$expr: {$gt: [{$getField: {field:"amount", input:
        // {$arrayElemAt: ["$bids", -1]}}}, 10]}})
        return Result.ok(bidDao);
    }

    public Result<List<BidDao>, ServiceError> getAuctionBids(ObjectId auctionId, int skip, int limit) {
        var aggregation = List.of(
                Aggregates.match(Filters.eq("_id", auctionId)),
                Aggregates.unwind("bids"),
                Aggregates.skip(skip),
                Aggregates.limit(limit));
        var bids = this.auctionCollection.aggregate(aggregation, BidDao.class).into(new ArrayList<BidDao>());
        return Result.ok(bids);
    }

    public Result<List<BidDao>, ServiceError> getAuctionBidsFiltered(ObjectId auctionId, List<ObjectId> bidids) {
        var aggregation = List.of(
                Aggregates.match(Filters.eq("_id", auctionId)),
                Aggregates.unwind("bids"),
                Aggregates.match(Filters.in("_id", bidids)));
        var bids = this.auctionCollection.aggregate(aggregation, BidDao.class).into(new ArrayList<BidDao>());
        return Result.ok(bids);
    }

    /* ------------------------- Question ------------------------- */

    /**
     * Get a question by its ID.
     * 
     * @param questionId The ID of the question.
     * @return the question, or null if it doesn't exist
     */
    public Result<QuestionDao, ServiceError> getQuestion(ObjectId questionId) {
        var question = this.questionCollection.find(Filters.eq("_id", questionId)).first();
        if (question == null)
            return Result.err(ServiceError.QUESTION_NOT_FOUND);
        logger.fine("getQuestion: " + question);
        return Result.ok(question);
    }

    /**
     * Create a new question.
     * Required fields: auctionId, userId, userIdDisplay, question, createTime.
     * 
     * @param questionDao Question to create
     * @return Created question
     */
    public Result<QuestionDao, ServiceError> createQuestion(QuestionDao questionDao) {
        assert questionDao.id == null : "Question ID must be null";
        assert questionDao.auctionId != null : "Question auction ID must not be null";
        assert questionDao.userId != null : "Question user ID must not be null";
        assert questionDao.userIdDisplay != null : "Question user ID display must not be null";
        assert questionDao.question != null : "Question question must not be null";
        assert questionDao.createTime != null : "Question create time must not be null";

        this.questionCollection.insertOne(questionDao);
        assert questionDao.id != null;
        return Result.ok(questionDao);
    }

    public Result<QuestionDao, ServiceError> createReply(ObjectId questionId, QuestionDao.Reply reply) {
        assert reply.reply != null : "Reply reply must not be null";
        assert reply.createTime != null : "Reply create time must not be null";
        assert reply.userIdDisplay != null : "Reply user ID display must not be null";

        var filter = Filters.and(Filters.eq("_id", questionId), Filters.eq("reply", null));
        var update = Updates.set("reply", reply);
        var updated = this.questionCollection.findOneAndUpdate(filter, update);
        if (updated == null)
            return Result.err(ServiceError.QUESTION_ALREADY_REPLIED);

        return this.getQuestion(questionId);
    }

    public Result<List<QuestionDao>, ServiceError> getAuctionQuestions(ObjectId auctionId) {
        var questions = this.questionCollection.find(Filters.eq("auction_id", auctionId)).into(new ArrayList<>());
        return Result.ok(questions);
    }

    /* ------------------------- User ------------------------- */

    public Result<UserDao, ServiceError> getUser(ObjectId userId) {
        var filter = Filters.eq("_id", userId);
        var projection = userDaoProjection();
        var userDao = this.userCollection.find(filter).projection(projection).first();
        if (userDao == null)
            return Result.err(ServiceError.USER_NOT_FOUND);
        return Result.ok(userDao);
    }

    public Result<UserDao, ServiceError> getUserByUsername(String username) {
        var filter = Filters.eq("username", username);
        var projection = userDaoProjection();
        var userDao = this.userCollection.find(filter).projection(projection).first();
        if (userDao == null)
            return Result.err(ServiceError.USER_NOT_FOUND);
        return Result.ok(userDao);
    }

    /**
     * Create a new user.
     * Required fields: username, hashedPassword, status, createTime.
     * 
     * @param userDao The user to create.
     * @return the created user, or null if the username is already taken
     */
    public Result<UserDao, ServiceError> createUser(UserDao userDao) {
        assert userDao.id == null : "User ID must be null";
        assert userDao.username != null : "Username must not be null";
        assert userDao.name != null : "Name must not be null";
        assert userDao.hashedPassword != null : "Hashed password must not be null";
        assert userDao.status == UserDao.Status.ACTIVE : "User status must be ACTIVE";
        assert userDao.createTime != null : "Create time must not be null";

        try {
            this.userCollection.insertOne(userDao);
            logger.fine("createUser: " + userDao);
            return Result.ok(userDao);
        } catch (MongoWriteException e) {
            logger.fine("createUser: user already exists");
            logger.fine(e.toString());
            return Result.err(ServiceError.USER_ALREADY_EXISTS);
        }
    }

    public Result<UserDao, ServiceError> updateUser(ObjectId userId, UserDao userDao) {
        assert userDao.id == null : "userDao.id must be null";
        assert userDao.username == null : "userDao.username must be null";
        assert userDao.status == null : "userDao.status must be null";
        assert userDao.createTime == null : "userDao.createTime must be null";

        var updates = new ArrayList<Bson>();
        if (userDao.name != null)
            updates.add(Updates.set("name", userDao.name));
        if (userDao.hashedPassword != null)
            updates.add(Updates.set("hashed_password", userDao.hashedPassword));
        if (userDao.profileImageId != null)
            updates.add(Updates.set("profile_image_id", userDao.profileImageId));

        var updated = this.userCollection.findOneAndUpdate(Filters.eq("_id", userId), updates);
        if (updated == null)
            return Result.err(ServiceError.USER_NOT_FOUND);

        logger.fine("updateUser: " + updated);

        return this.getUser(userId);
    }

    public Result<UserDao, ServiceError> deactivateUser(ObjectId userId) {
        logger.fine("deactivateUser: attempting to deactivate " + userId);
        var updated = this.userCollection.findOneAndUpdate(
                Filters.eq("_id", userId),
                Updates.set("status", "INACTIVE"));
        if (updated == null) {
            logger.fine("deactivateUser: user not found or already inactive");
            return Result.err(ServiceError.USER_NOT_FOUND);
        }
        logger.fine("deactivateUser: deactivated " + userId);
        return Result.ok(updated);
    }

    public Result<List<AuctionDao>, ServiceError> getUserAuctions(ObjectId userId) {
        var auctions = this.auctionCollection.find(Filters.eq("user_id", userId)).into(new ArrayList<>());
        return Result.ok(auctions);
    }

    /* ------------------------- Internal ------------------------- */

    private static Bson auctionDaoProjection() {
        return Projections.exclude("bids");
    }

    private static Bson userDaoProjection() {
        return Projections.exclude("created_auctions", "created_bids");
    }
}
