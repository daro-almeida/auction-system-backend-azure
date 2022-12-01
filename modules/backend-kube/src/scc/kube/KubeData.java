package scc.kube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.types.ObjectId;

import redis.clients.jedis.Jedis;
import scc.AuctionStatus;
import scc.PagingWindow;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.item.UserItem;
import scc.kube.config.KubeConfig;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.QuestionDao;
import scc.kube.dao.UserDao;

public class KubeData {
    private static final Logger logger = Logger.getLogger(KubeData.class.getName());

    private final KubeConfig config;
    private final Mongo mongo;
    private final Jedis jedis;

    public KubeData(KubeConfig config, Mongo mongo, Jedis jedis) {
        this.config = config;
        this.mongo = mongo;
        this.jedis = jedis;
    }

    /* ------------------------- Auction ------------------------- */

    public Result<AuctionDao, ServiceError> getAuction(ObjectId auctionId) {
        if (this.config.isCachingEnabled()) {
            var auctionDao = Redis.getAuction(this.jedis, auctionId);
            if (auctionDao != null)
                return Result.ok(auctionDao);
        }

        var result = this.mongo.getAuction(auctionId);
        if (result.isError())
            return Result.err(result);

        var auctionDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setAuction(this.jedis, auctionDao);

        return Result.ok(auctionDao);
    }

    public Result<Map<ObjectId, AuctionDao>, ServiceError> getAuctionMany(List<ObjectId> auctionIds) {
        var auctionDaos = new HashMap<ObjectId, AuctionDao>();
        var missingAuctionIds = new HashSet<>(auctionIds);
        if (this.config.isCachingEnabled()) {
            var auctionDaoMap = Redis.getAuctionMany(this.jedis, auctionIds);
            auctionDaos.putAll(auctionDaoMap);
            for (var auctionId : auctionDaoMap.keySet())
                missingAuctionIds.remove(auctionId);
        }

        if (missingAuctionIds.isEmpty())
            return Result.ok(auctionDaos);

        var result = this.mongo.getAuctionMany(new ArrayList<>(missingAuctionIds));
        if (result.isError())
            return Result.err(result);
        var missingAuctionDaos = result.value();

        auctionDaos.putAll(missingAuctionDaos);
        if (this.config.isCachingEnabled())
            Redis.setAuctionMany(this.jedis, new ArrayList<>(missingAuctionDaos.values()));

        return Result.ok(auctionDaos);
    }

    public Result<AuctionDao, ServiceError> createAuction(AuctionDao auctionDao) {
        auctionDao = this.mongo.createAuction(auctionDao);
        if (this.config.isCachingEnabled()) {
            Redis.setAuction(this.jedis, auctionDao);
            Redis.pushUserAuction(jedis, auctionDao.userId, auctionDao.id);
        }
        Redis.pushRecentAuction(jedis, auctionDao.id);
        return Result.ok(auctionDao);
    }

    public Result<AuctionDao, ServiceError> updateAuction(ObjectId auctionId, AuctionDao auctionDao) {
        var updateResult = this.mongo.updateAuction(auctionId, auctionDao);
        if (updateResult.isError())
            return Result.err(updateResult);

        if (this.config.isCachingEnabled())
            Redis.setAuction(this.jedis, auctionDao);

        return Result.ok(auctionDao);
    }

    public Result<AuctionDao, ServiceError> closeAuction(ObjectId auctionId) {
        var result = this.mongo.closeAuction(auctionId);
        if (result.isError())
            return Result.err(result);

        var auctionDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setAuction(this.jedis, auctionDao);

        return Result.ok(auctionDao);
    }

    public Result<List<BidDao>, ServiceError> getAuctionBids(ObjectId auctionId, int skip, int limit) {
        // TODO: this could be cached
        return this.mongo.getAuctionBids(auctionId, skip, limit);
    }

    public Result<List<AuctionDao>, ServiceError> getRecentAuctions() {
        var auctionIds = Redis.getRecentAuctionIds(this.jedis);
        var auctionDaosResult = this.getAuctionMany(auctionIds);
        if (auctionDaosResult.isError())
            return Result.err(auctionDaosResult);
        var auctionDaos = auctionDaosResult.value();
        return Result.ok(auctionIds.stream().map(auctionDaos::get).filter(Objects::nonNull).toList());
    }

    public Result<List<AuctionDao>, ServiceError> getAuctionsSoonToClose() {
        var auctionIds = Redis.getSoonToCloseAuctionIds(this.jedis);
        var auctionDaosResult = this.getAuctionMany(auctionIds);
        if (auctionDaosResult.isError())
            return Result.err(auctionDaosResult);
        var auctionDaos = auctionDaosResult.value();
        return Result.ok(auctionIds.stream().map(auctionDaos::get).filter(Objects::nonNull).toList());
    }

    public Result<List<AuctionDao>, ServiceError> getPopularAuctions() {
        var auctionIds = Redis.getPopularAuctions(this.jedis);
        var auctionDaosResult = this.getAuctionMany(auctionIds);
        if (auctionDaosResult.isError())
            return Result.err(auctionDaosResult);
        var auctionDaos = auctionDaosResult.value();
        return Result.ok(auctionIds.stream().map(auctionDaos::get).filter(Objects::nonNull).toList());
    }

    public AuctionItem auctionDaoToItem(AuctionDao auctionDao) {
        var topBidId = Redis.getAuctionTopBid(this.jedis, auctionDao.id);
        var topBid = topBidId == null ? Optional.<BidDao>empty() : this.getAuctionBid(auctionDao.id, topBidId);
        return auctionDaoToItem(auctionDao, topBid);
    }

    public Map<ObjectId, AuctionItem> auctionDaoToItemMany(List<AuctionDao> auctionDaos) {
        // TODO: this can be improved later
        var items = new HashMap<ObjectId, AuctionItem>();
        for (var auctionDao : auctionDaos)
            items.put(auctionDao.id, auctionDaoToItem(auctionDao));
        return items;
    }

    public AuctionItem auctionDaoToItem(AuctionDao auctionDao, Optional<BidDao> highestBidDao) {
        return new AuctionItem(
                auctionDao.id.toHexString(),
                auctionDao.title,
                auctionDao.description,
                auctionDao.userIdDisplay,
                auctionDao.createTime,
                auctionDao.closeTime,
                Optional.ofNullable(auctionDao.imageId).map(Kube::stringToMediaId),
                auctionDao.initialPrice,
                auctionDaoStatusToAuctionStatus(auctionDao.status),
                highestBidDao.map(b -> this.bidDaoToItem(auctionDao.id, b)));
    }

    private static AuctionStatus auctionDaoStatusToAuctionStatus(AuctionDao.Status status) {
        return switch (status) {
            case CLOSED -> AuctionStatus.CLOSED;
            case OPEN -> AuctionStatus.OPEN;
            default -> throw new IllegalStateException();
        };
    }

    /* ------------------------- Bid ------------------------- */

    public Result<BidDao, ServiceError> createBid(ObjectId auctionId, BidDao bidDao) {
        var result = this.mongo.createBid(auctionId, bidDao);
        if (result.isError())
            return Result.err(result);

        bidDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setBid(this.jedis, bidDao);

        // This is always used, with caching or not
        Redis.setAuctionTopBid(jedis, auctionId, bidDao.id);

        return Result.ok(bidDao);
    }

    public Optional<BidDao> getAuctionBid(ObjectId auctionId, ObjectId bidId) {
        if (this.config.isCachingEnabled()) {
            var bidDao = Redis.getBid(this.jedis, bidId);
            if (bidDao != null)
                return Optional.of(bidDao);
        }

        var result = this.mongo.getBid(auctionId, bidId);
        if (result.isError())
            return Optional.empty();

        var bidDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setBid(this.jedis, bidDao);

        return Optional.of(bidDao);
    }

    public BidItem bidDaoToItem(ObjectId auctionId, BidDao bidDao) {
        return new BidItem(
                bidDao.id.toHexString(),
                auctionId.toHexString(),
                bidDao.userIdDisplay,
                bidDao.createTime,
                bidDao.amount);
    }

    /* ------------------------- Question ------------------------- */

    public Result<QuestionDao, ServiceError> createQuestion(QuestionDao questionDao) {
        var result = this.mongo.createQuestion(questionDao);
        if (result.isError())
            return Result.err(result);

        questionDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setQuestion(this.jedis, questionDao);

        return Result.ok(questionDao);
    }

    public Result<QuestionDao, ServiceError> createReply(ObjectId questionId, QuestionDao.Reply reply) {
        var result = this.mongo.createReply(questionId, reply);
        if (result.isError())
            return Result.err(result);

        var questionDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setQuestion(this.jedis, questionDao);

        return Result.ok(questionDao);
    }

    public Result<List<QuestionDao>, ServiceError> getAuctionQuestions(ObjectId auctionId, PagingWindow window) {
        // TODO: maybe cache this?
        return this.mongo.getAuctionQuestions(auctionId, window);
    }

    public QuestionItem questionDaoToItem(QuestionDao questionDao) {
        return new QuestionItem(
                questionDao.id.toHexString(),
                questionDao.auctionId.toHexString(),
                questionDao.userIdDisplay,
                questionDao.question,
                Optional.ofNullable(questionDao.reply).map(r -> new ReplyItem(
                        questionDao.id.toHexString(),
                        r.userIdDisplay,
                        r.reply)));
    }

    public ReplyItem questionDaoToReplyItem(QuestionDao questionDao) {
        return new ReplyItem(
                questionDao.id.toHexString(),
                questionDao.reply.userIdDisplay,
                questionDao.reply.reply);
    }

    /* ------------------------- User ------------------------- */

    public Result<UserDao, ServiceError> getUser(ObjectId userId) {
        if (this.config.isCachingEnabled()) {
            var userDao = Redis.getUser(this.jedis, userId);
            if (userDao != null)
                return Result.ok(userDao);
        }

        var result = this.mongo.getUser(userId);
        if (result.isError())
            return Result.err(result);

        var userDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setUser(this.jedis, userDao);

        return Result.ok(userDao);
    }

    public Result<UserDao, ServiceError> getUserByUsername(String username) {
        var result = this.mongo.getUserByUsername(username);
        if (result.isError())
            return Result.err(result);

        var userDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setUser(this.jedis, userDao);

        return Result.ok(userDao);
    }

    public Result<UserDao, ServiceError> createUser(UserDao userDao) {
        var result = this.mongo.createUser(userDao);
        if (result.isError())
            return Result.err(result);

        userDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setUser(this.jedis, userDao);

        return Result.ok(userDao);
    }

    public Result<UserDao, ServiceError> updateUser(ObjectId userId, UserDao userDao) {
        var result = this.mongo.updateUser(userId, userDao);
        if (result.isError())
            return Result.err(result);

        userDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.unsetUser(this.jedis, userId);

        return Result.ok(userDao);
    }

    public Result<UserDao, ServiceError> deactivateUser(ObjectId userId) {
        var result = this.mongo.deactivateUser(userId);
        if (result.isError())
            return Result.err(result);

        var userDao = result.value();
        if (this.config.isCachingEnabled())
            Redis.setUser(this.jedis, userDao);

        return Result.ok(userDao);
    }

    public Result<List<AuctionDao>, ServiceError> getUserAuctions(ObjectId userId) {
        if (this.config.isCachingEnabled()) {
            var auctionIds = Redis.getUserAuctions(this.jedis, userId);
            if (auctionIds != null)
                return this.getAuctionMany(auctionIds).map(map -> new ArrayList<>(map.values()));
        }

        var result = this.mongo.getUserAuctions(userId);
        if (result.isError())
            return Result.err(result);

        var auctionDaos = result.value();
        if (this.config.isCachingEnabled()) {
            Redis.setAuctionMany(jedis, auctionDaos);
            Redis.setUserAuctions(this.jedis, userId, auctionDaos.stream().map(a -> a.id).toList());
        }

        return Result.ok(auctionDaos);
    }

    public Result<List<AuctionDao>, ServiceError> getAuctionsFollowedByUser(ObjectId userId) {
        // Redis either stores all user followed auctions or none of them
        if (this.config.isCachingEnabled()) {
            var auctionIds = Redis.getUserFollowedAuctions(this.jedis, userId);
            if (!auctionIds.isEmpty())
                return this.getAuctionMany(auctionIds).map(map -> new ArrayList<>(map.values()));
        }

        var result = this.mongo.getUserBidIds(userId);
        if (result.isError())
            return Result.err(result);

        var auctionIds = result.value();
        if (auctionIds.isEmpty())
            return Result.ok(List.of());

        Redis.addUserFollowedAuctions(this.jedis, userId, auctionIds);

        var auctionsResult = this.getAuctionMany(auctionIds);
        if (auctionsResult.isError())
            return Result.err(auctionsResult);

        var auctions = new ArrayList<>(auctionsResult.value().values());
        if (this.config.isCachingEnabled())
            Redis.setAuctionMany(this.jedis, auctions);

        return Result.ok(auctions);
    }

    public UserItem userDaoToItem(UserDao userDao) {
        return new UserItem(
                userDao.username,
                userDao.name,
                Optional.ofNullable(userDao.profileImageId).map(Kube::stringToMediaId));
    }

    /* ------------------------- Auth ------------------------- */

    public Result<SessionToken, ServiceError> authenticate(String username, String password) {
        var getUserResult = this.getUserByUsername(username);
        if (getUserResult.isError())
            return Result.err(getUserResult);
        var userDao = getUserResult.value();

        if (!userDao.hashedPassword.equals(Kube.hashUserPassword(password)))
            return Result.err(ServiceError.INVALID_CREDENTIALS);

        var token = new SessionToken(UUID.randomUUID().toString());
        Redis.setSession(this.jedis, userDao.id, token.getToken());
        return Result.ok(token);
    }

    public Result<ObjectId, ServiceError> validate(SessionToken token) {
        logger.fine("Validating token: " + token.getToken());
        var userId = Redis.getSession(this.jedis, token.getToken());
        if (userId == null) {
            logger.fine("Token not found");
            return Result.err(ServiceError.UNAUTHORIZED);
        }
        logger.fine("Token found, userId: " + userId);
        return Result.ok(userId);
    }
}
