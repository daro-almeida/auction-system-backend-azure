package scc.kube;

import java.util.Optional;
import java.util.logging.Logger;

import org.bson.types.ObjectId;

import redis.clients.jedis.JedisPool;
import scc.AppLogic;
import scc.Result;
import scc.ServiceError;
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

    public UserItem userDaoToItem(UserDao userDao) {
        if (userDao.getStatus() == UserDao.Status.ACTIVE) {
            return new UserItem(
                    userDao.getUserId(),
                    userDao.getName(),
                    null); // TODO: Fix this
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
}
