package scc.kube;

import java.io.Closeable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

import io.opentelemetry.instrumentation.annotations.WithSpan;
import scc.PagingWindow;
import scc.Result;
import scc.ServiceError;
import scc.exception.AuctionNotFoundException;
import scc.exception.BidConflictException;
import scc.exception.BidNotFoundException;
import scc.exception.QuestionAlreadyRepliedException;
import scc.exception.QuestionNotFoundException;
import scc.exception.UserAlreadyExistsException;
import scc.exception.UserNotFoundException;
import scc.kube.config.MongoConfig;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.AuctionIdWithBidDao;
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
        this.auctionCollection.createIndex(Indexes.descending("bids.user_id"));

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

    @WithSpan
    public AuctionDao getAuction(ObjectId auctionId) throws AuctionNotFoundException {
        var filter = Filters.eq("_id", auctionId);
        var projection = auctionDaoProjection();
        var auctionDao = this.auctionCollection.find(filter).projection(projection).first();
        if (auctionDao == null)
            throw new AuctionNotFoundException(auctionId.toHexString());
        return auctionDao;
    }

    @WithSpan
    public HashMap<ObjectId, AuctionDao> getAuctionMany(List<ObjectId> auctionIds) {
        var filter = Filters.in("_id", auctionIds);
        var projection = auctionDaoProjection();
        var auctionDaos = this.auctionCollection.find(filter).projection(projection).into(new ArrayList<AuctionDao>());
        var auctionMap = new HashMap<ObjectId, AuctionDao>();
        for (var auctionDao : auctionDaos)
            auctionMap.put(auctionDao.id, auctionDao);
        return auctionMap;
    }

    /**
     * Create a new auction.
     * Required fields: title, description, userid,
     * createTime,closeTime,initialprice,status.
     * 
     * @param auctionDao
     * @return
     */
    @WithSpan
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

    @WithSpan
    public AuctionDao updateAuction(ObjectId auctionId, AuctionDao auctionDao) throws AuctionNotFoundException {
        assert auctionDao.id == null : "Auction ID must be null";
        assert auctionDao.userId == null : "Auction user ID must be null";
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
            throw new AuctionNotFoundException(auctionId.toHexString());

        logger.fine("updateAuction: " + updated);
        return updated;
    }

    @WithSpan
    public Map<ObjectId, BidDao> getAuctionTopBidMany(Collection<ObjectId> auctionIds) {
        var filter = Filters.in("_id", auctionIds);
        var projection = Projections.fields(
                Projections.include("_id"),
                new Document("bid", new Document("$first", "$bids")));
        var daos = this.auctionCollection.find(filter, AuctionIdWithBidDao.class).projection(projection);
        var map = new HashMap<ObjectId, BidDao>();
        for (var dao : daos) {
            if (dao.bid != null)
                map.put(dao.id, dao.bid);
        }
        return map;
    }

    @WithSpan
    public AuctionDao closeAuction(ObjectId auctionId) throws AuctionNotFoundException {
        logger.fine("closeAuction: trying to close" + auctionId);
        var updated = this.auctionCollection.findOneAndUpdate(
                Filters.eq("_id", auctionId),
                Updates.set("status", AuctionDao.Status.CLOSED.toString()));
        if (updated == null)
            throw new AuctionNotFoundException(auctionId.toHexString());
        updated.status = AuctionDao.Status.CLOSED;
        logger.fine("closeAuction: closed " + updated);
        return updated;
    }

    /**
     * Get all auctions that are currently open and should close before `before`.
     * 
     * @param before The time before which auctions should close.
     * @return A list of auctions that are about to close.
     */
    @WithSpan
    public List<AuctionDao> getAuctionSoonToClose(LocalDateTime before) {
        var filter = Filters.and(
                Filters.eq("status", AuctionDao.Status.OPEN.toString()),
                Filters.lt("close_time", before));
        var projection = auctionDaoProjection();
        return this.auctionCollection.find(filter).projection(projection).into(new ArrayList<AuctionDao>());
    }

    /* ------------------------- Bid ------------------------- */

    @WithSpan
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

    @WithSpan
    public Map<ObjectId, BidDao> getBidMany(Collection<ObjectId> bidIds) throws BidNotFoundException {
        var pipeline = Arrays.asList(
                Aggregates.match(Filters.in("bids._id", bidIds)),
                Aggregates.unwind("$bids"),
                Aggregates.replaceRoot("$bids"),
                Aggregates.match(Filters.in("_id", bidIds)));
        var bidDaos = this.auctionCollection.aggregate(pipeline, BidDao.class).into(new ArrayList<BidDao>());
        var bidMap = new HashMap<ObjectId, BidDao>();
        for (var bidDao : bidDaos)
            bidMap.put(bidDao.id, bidDao);
        if (bidMap.size() != bidIds.size())
            throw new BidNotFoundException();
        return bidMap;
    }

    /**
     * Create a new bid.
     * Required fields: userId, userIdDisplay, amount, createTime.
     * 
     * @param auctionId ID of the auction to bid on
     * @param bidDao    Bid to create
     * @return Created bid
     */
    @WithSpan
    public BidDao createBid(ObjectId auctionId, BidDao bidDao) throws BidConflictException {
        assert bidDao.id == null : "Bid ID must be null";
        assert bidDao.auctionId == null || bidDao.auctionId.equals(auctionId) : "Bid auction ID must match auction ID";
        assert bidDao.userId != null : "Bid user ID must not be null";
        assert bidDao.amount >= 0 : "Bid amount must be non-negative";
        assert bidDao.createTime != null : "Bid create time must not be null";

        bidDao.id = new ObjectId();
        bidDao.auctionId = auctionId;
        var filterLess = new Document().append("$expr", new Document("$lt", Arrays.asList(
                new Document().append("$getField", new Document("field", "amount").append("input", new Document(
                        "$arrayElemAt", Arrays.asList("$bids", -1)))),
                bidDao.amount)));

        var filter = Filters.and(
                Filters.eq("_id", auctionId),
                Filters.or(filterLess, Filters.exists("bids", false)),
                Filters.eq("status", AuctionDao.Status.OPEN.toString()));

        var updated = this.auctionCollection.findOneAndUpdate(filter, Updates.push("bids", bidDao));
        if (updated == null)
            throw new BidConflictException();

        // db.auctions.find({$expr: {$gt: [{$getField: {field:"amount", input:
        // {$arrayElemAt: ["$bids", -1]}}}, 10]}})
        return bidDao;
    }

    public List<BidDao> getAuctionBids(ObjectId auctionId, PagingWindow window) {
        var aggregation = List.of(
                Aggregates.match(Filters.eq("_id", auctionId)),
                Aggregates.unwind("$bids"),
                Aggregates.skip(window.skip),
                Aggregates.limit(window.limit));
        var bids = this.auctionCollection.aggregate(aggregation, BidDao.class).into(new ArrayList<BidDao>());
        return bids;
    }

    /* ------------------------- Question ------------------------- */

    /**
     * Get a question by its ID.
     * 
     * @param questionId The ID of the question.
     * @return the question, or null if it doesn't exist
     */
    @WithSpan
    public QuestionDao getQuestion(ObjectId questionId) throws QuestionNotFoundException {
        var question = this.questionCollection.find(Filters.eq("_id", questionId)).first();
        if (question == null)
            throw new QuestionNotFoundException();
        logger.fine("getQuestion: " + question);
        return question;
    }

    /**
     * Create a new question.
     * Required fields: auctionId, userId, userIdDisplay, question, createTime.
     * 
     * @param questionDao Question to create
     * @return Created question
     */
    @WithSpan
    public QuestionDao createQuestion(QuestionDao questionDao) {
        assert questionDao.id == null : "Question ID must be null";
        assert questionDao.auctionId != null : "Question auction ID must not be null";
        assert questionDao.userId != null : "Question user ID must not be null";
        assert questionDao.question != null : "Question question must not be null";
        assert questionDao.createTime != null : "Question create time must not be null";

        this.questionCollection.insertOne(questionDao);
        assert questionDao.id != null;
        return questionDao;
    }

    @WithSpan
    public QuestionDao createReply(ObjectId questionId, QuestionDao.Reply reply)
            throws QuestionNotFoundException, QuestionAlreadyRepliedException {
        assert reply.reply != null : "Reply reply must not be null";
        assert reply.createTime != null : "Reply create time must not be null";
        assert reply.userId != null : "Reply user ID must not be null";

        var filter = Filters.and(Filters.eq("_id", questionId), Filters.eq("reply", null));
        var update = Updates.set("reply", reply);
        var updated = this.questionCollection.findOneAndUpdate(filter, update);
        if (updated == null)
            throw new QuestionAlreadyRepliedException();

        return this.getQuestion(questionId);
    }

    @WithSpan
    public List<QuestionDao> getAuctionQuestions(ObjectId auctionId, PagingWindow window) {
        var questions = this.questionCollection.find(Filters.eq("auction_id", auctionId))
                .skip(window.skip).limit(window.limit).into(new ArrayList<>());
        return questions;
    }

    /* ------------------------- User ------------------------- */

    @WithSpan
    public UserDao getUser(ObjectId userId) throws UserNotFoundException {
        var filter = Filters.eq("_id", userId);
        var projection = userDaoProjection();
        var userDao = this.userCollection.find(filter).projection(projection).first();
        if (userDao == null)
            throw new UserNotFoundException();
        return userDao;
    }

    @WithSpan
    public Map<ObjectId, UserDao> getUserMany(Collection<ObjectId> userIds) throws UserNotFoundException {
        var filter = Filters.in("_id", userIds);
        var projection = userDaoProjection();
        var userDao = this.userCollection.find(filter).projection(projection).into(new ArrayList<>());
        var map = userDao.stream().collect(Collectors.toMap(u -> u.id, u -> u));
        if (map.size() != userIds.size())
            throw new UserNotFoundException();
        return map;
    }

    @WithSpan
    public UserDao getUserByUsername(String username)
            throws UserNotFoundException {
        var filter = Filters.eq("username", username);
        var projection = userDaoProjection();
        var userDao = this.userCollection.find(filter).projection(projection).first();
        if (userDao == null)
            throw new UserNotFoundException(username);
        return userDao;
    }

    /**
     * Create a new user.
     * Required fields: username, hashedPassword, status, createTime.
     * 
     * @param userDao The user to create.
     * @return the created user, or null if the username is already taken
     */
    @WithSpan
    public UserDao createUser(UserDao userDao)
            throws UserAlreadyExistsException {
        assert userDao.id == null : "User ID must be null";
        assert userDao.username != null : "Username must not be null";
        assert userDao.name != null : "Name must not be null";
        assert userDao.hashedPassword != null : "Hashed password must not be null";
        assert userDao.status == UserDao.Status.ACTIVE : "User status must be ACTIVE";
        assert userDao.createTime != null : "Create time must not be null";

        try {
            this.userCollection.insertOne(userDao);
            logger.fine("createUser: " + userDao);
            return userDao;
        } catch (MongoWriteException e) {
            logger.fine("createUser: user already exists");
            logger.fine(e.toString());
            throw new UserAlreadyExistsException(userDao.username);
        }
    }

    @WithSpan
    public UserDao updateUser(ObjectId userId, UserDao userDao) throws UserNotFoundException {
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
            throw new UserNotFoundException();

        logger.fine("updateUser: " + updated);

        return this.getUser(userId);
    }

    @WithSpan
    public UserDao deactivateUser(ObjectId userId) throws UserNotFoundException {
        logger.fine("deactivateUser: attempting to deactivate " + userId);
        var updated = this.userCollection.findOneAndUpdate(
                Filters.eq("_id", userId),
                Updates.set("status", "INACTIVE"));
        if (updated == null) {
            logger.fine("deactivateUser: user not found or already inactive");
            throw new UserNotFoundException();
        }
        logger.fine("deactivateUser: deactivated " + userId);
        return updated;
    }

    @WithSpan
    public List<AuctionDao> getUserAuctions(ObjectId userId) {
        var auctions = this.auctionCollection.find(Filters.eq("user_id", userId)).into(new ArrayList<>());
        return auctions;
    }

    @WithSpan
    public List<ObjectId> getUserBidIds(ObjectId userId) {
        var pipeline = Arrays.asList(
                Aggregates.match(Filters.eq("_id", userId)),
                Aggregates.project(Projections.include("bids")),
                Aggregates.unwind("$bids"),
                Aggregates.replaceRoot("$bids"));
        var bidIds = this.userCollection.aggregate(pipeline, ObjectId.class).into(new ArrayList<ObjectId>());
        return bidIds;
    }

    @WithSpan
    public List<AuctionDao> getAuctionsFollowedByUser(ObjectId userId) {
        var pipeline = Arrays.asList(
                Aggregates.match(Filters.in("bids.user_id", userId)),
                Aggregates.project(auctionDaoProjection()));
        var auctions = this.auctionCollection.aggregate(pipeline, AuctionDao.class).into(new ArrayList<>());
        return auctions;
    }

    /* ------------------------- Internal ------------------------- */

    private static Bson auctionDaoProjection() {
        return Projections.exclude("bids");
    }

    private static Bson userDaoProjection() {
        return Projections.exclude();
    }
}
