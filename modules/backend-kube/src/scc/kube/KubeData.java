package scc.kube;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import scc.AppLogic;
import scc.AuctionStatus;
import scc.Result;
import scc.ServiceError;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.UserItem;
import scc.kube.config.KubeConfig;
import scc.kube.dao.*;

public class KubeData {
    private static final Logger logger = Logger.getLogger(KubeData.class.getName());

    private final KubeConfig config;
    private final Mongo mongo;
    private final JedisPool jedisPool;

    public KubeData(KubeConfig config, Mongo mongo, JedisPool jedisPool) {
        this.config = config;
        this.mongo = mongo;
        this.jedisPool = jedisPool;
    }

    /* ------------------------- Auction DAO ------------------------- */

    public void setAuction(AuctionDao auctionDao) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                Redis.setAuction(jedis, auctionDao);
            }
        }
    }

    public Result<AuctionDao, ServiceError> getAuction(ObjectId auctionId) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                var auctionDao = Redis.getAuction(jedis, auctionId);
                if (auctionDao != null) {
                    return Result.ok(auctionDao);
                }
            }
        }

        var auctionDao = this.mongo.getAuction(auctionId);
        if (auctionDao == null) {
            return Result.err(ServiceError.AUCTION_NOT_FOUND);
        }

        this.setAuction(auctionDao);
        return Result.ok(auctionDao);
    }

    public void invalidateAuction(ObjectId auctionId) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                Redis.removeAuction(jedis, auctionId);
            }
        }
    }

    /* ------------------------- Auction Item ------------------------- */

    public Result<AuctionItem, ServiceError> auctionDaoToItem(AuctionDao auctionDao) {
        var item = new AuctionItem(
                auctionDao.getId().toHexString(),
                auctionDao.getTitle(),
                auctionDao.getDescription(),
                auctionDao.getUserId(),
                auctionDao.getCloseTime(),
                Optional.empty(), // TODO: fix this
                auctionDao.getInitialPrice(),
                auctionDaoStatusToAuctionStatus(auctionDao.getStatus()),
                Optional.empty() // TODO: fix this
        );
        return Result.ok(item);
    }

    public static AuctionStatus auctionDaoStatusToAuctionStatus(AuctionDao.Status status) {
        return switch (status) {
            case CLOSED -> AuctionStatus.CLOSED;
            case DELETED -> AuctionStatus.CLOSED;
            case OPEN -> AuctionStatus.OPEN;
            default -> throw new IllegalStateException("Unknown auction status: " + status);
        };
    }

    /* ------------------------- Bid DAO ------------------------- */

    public void setBid(BidDao bidDao) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                Redis.setBid(jedis, bidDao);
            }
        }
    }

    public Result<BidDao, ServiceError> getBid(ObjectId bidId) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                var bidDao = Redis.getBid(jedis, bidId);
                if (bidDao != null) {
                    return Result.ok(bidDao);
                }
            }
        }

        var bidDao = this.mongo.getBid(bidId);
        if (bidDao == null) {
            return Result.err(ServiceError.BID_NOT_FOUND);
        }

        this.setBid(bidDao);
        return Result.ok(bidDao);
    }

    private static List<BidDao> getBidMany(Mongo mongo, Jedis jedis, List<ObjectId> bidIds) {
        var auctionBidIdsSet = new HashSet<>(bidIds);
        var cachedBidDaos = Redis.getBidMany(jedis, bidIds);
        var missingBidIds = cachedBidDaos.keySet().stream()
                .filter(nullBidId -> !auctionBidIdsSet.contains(nullBidId)).toList();
        var missingBidDaos = mongo.getBidMany(missingBidIds);
        return Stream.concat(cachedBidDaos.values().stream(), missingBidDaos.values().stream())
                .collect(Collectors.toList());
    }

    /* ------------------------- Bid Item ------------------------- */

    public BidItem bidDaoToItem(BidDao bidDao, UserDao userDao) {
        var item = new BidItem(
                bidDao.getId().toHexString(),
                bidDao.getAuctionId().toHexString(),
                userDaoDisplayId(userDao),
                bidDao.getTime(),
                bidDao.getValue().doubleValue());
        return item;
    }

    public Result<List<BidItem>, ServiceError> listAuctionBidItems(ObjectId auctionId, int skip, int limit) {
        // Database Only
        if (!this.config.isCachingEnabled())
            return this.listAuctionBidItemsFromDatabase(auctionId, skip, limit);

        // Cache Assisted
        try (var jedis = this.jedisPool.getResource()) {
            var auctionBidIds = Redis.getAuctionBids(jedis, auctionId, skip, limit);
            if (auctionBidIds == null)
                return this.listAuctionBidItemsFromDatabase(auctionId, skip, limit);

            var bidDaos = getBidMany(this.mongo, jedis, auctionBidIds);
            var requiredUserIds = bidDaos.stream().map(BidDao::getUserId).collect(Collectors.toSet()).stream().toList();

        }

        return null;
    }

    private Result<List<BidItem>, ServiceError> listAuctionBidItemsFromDatabase(ObjectId auctionId, int skip,
            int limit) {
        var bidDaos = this.mongo.listAuctionBids(auctionId, skip, limit);
        var userIds = bidDaos.stream().map(BidDao::getUserId).toList();
        var userDaos = this.mongo.getUserMany(userIds);
        var bidItems = bidDaos.stream().map(b -> bidDaoToItem(b, userDaos.get(b.getUserId()))).toList();
        return Result.ok(bidItems);
    }

    /* ------------------------- User DAO ------------------------- */

    public void setUser(UserDao userDao) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                Redis.setUser(jedis, userDao);
            }
        }
    }

    public Result<UserDao, ServiceError> getUser(String userId) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                var userDao = Redis.getUser(jedis, userId);
                if (userDao != null) {
                    return Result.ok(userDao);
                }
            }
        }

        var userDao = this.mongo.getUser(userId);
        if (userDao == null) {
            return Result.err(ServiceError.USER_NOT_FOUND);
        }

        this.setUser(userDao);
        return Result.ok(userDao);
    }

    /* ------------------------- User Item ------------------------- */

    public static UserItem userDaoToItem(UserDao userDao) {
        if (userDao.getStatus() == UserDao.Status.ACTIVE) {
            return new UserItem(
                    userDao.getUserId(),
                    userDao.getName(),
                    Optional.empty()); // TODO: Fix this
        } else {
            return new UserItem(
                    AppLogic.DELETED_USER_ID,
                    AppLogic.DELETED_USER_NAME,
                    Optional.empty());
        }
    }

    /* ------------------------- Question DAO ------------------------- */

    public void setQuestion(QuestionDao questionDao) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                Redis.setQuestion(jedis, questionDao);
            }
        }
    }

    public Result<QuestionDao, ServiceError> getQuestion(ObjectId questionId) {
        if (this.config.isCachingEnabled()) {
            try (var jedis = this.jedisPool.getResource()) {
                var questionDao = Redis.getQuestion(jedis, questionId);
                if (questionDao != null) {
                    return Result.ok(questionDao);
                }
            }
        }

        var questionDao = this.mongo.getQuestion(questionId);
        if (questionDao == null) {
            return Result.err(ServiceError.QUESTION_NOT_FOUND);
        }

        this.setQuestion(questionDao);
        return Result.ok(questionDao);
    }

    /* ------------------------- Internal ------------------------- */

    private static String userDaoDisplayId(UserDao userDao) {
        return userDao.getStatus() == UserDao.Status.ACTIVE ? userDao.getUserId() : AppLogic.DELETED_USER_ID;
    }
}
