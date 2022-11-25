package scc.kube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.types.ObjectId;

import redis.clients.jedis.Jedis;
import scc.AuctionStatus;
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

    public Result<HashMap<ObjectId, AuctionDao>, ServiceError> getAuctionMany(List<ObjectId> auctionIds) {
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
        if (this.config.isCachingEnabled()) {
            var bidIds = Redis.getAuctionBids(this.jedis, auctionId, skip, limit);
            if (bidIds != null) {

            }
        }
        return null;
    }

    public AuctionItem auctionDaoToItem(AuctionDao auctionDao) {
        // TODO: fix this
        // + Add a set/getAuctionTopBid method to Redis
        // + Add a set/getAuctionTopMany method to Redis
        // + Add these methods to Mongo
        // + ???
        // + Profit
        return auctionDaoToItem(auctionDao, Optional.empty());
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

        return Result.ok(bidDao);
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
