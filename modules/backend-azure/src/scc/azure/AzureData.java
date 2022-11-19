package scc.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.azure.cosmos.CosmosContainer;

import redis.clients.jedis.JedisPool;
import scc.AuctionStatus;
import scc.Result;
import scc.ServiceError;
import scc.azure.config.AzureConfig;
import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.item.UserItem;

public class AzureData {

    /* ------------------------- Auction DAO ------------------------- */

    public static void setAuction(AzureConfig config, JedisPool jedisPool, AuctionDAO auctionDao) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                Redis.setAuction(jedis, auctionDao);
            }
        }
    }

    public static Result<AuctionDAO, ServiceError> getAuction(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer,
            String auctionId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var auctionDao = Redis.getAuction(jedis, auctionId);
                if (auctionDao != null)
                    return Result.ok(auctionDao);
            }
        }

        var auctionDao = Cosmos.getAuction(auctionContainer, auctionId);
        if (auctionDao.isEmpty())
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        setAuction(config, jedisPool, auctionDao.get());
        return Result.ok(auctionDao.get());
    }

    public static Result<List<AuctionDAO>, ServiceError> listAuctionsFollowedByUser(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer,
            CosmosContainer bidContainer,
            String userId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var auctionIds = Redis.getUserAuctions(jedis, userId);
                if (auctionIds != null)
                    return Result.ok(auctionIdsToDaos(config, jedisPool, auctionContainer, auctionIds));
            }
        }
        var auctionIdsResult = Cosmos.listAuctionsFollowedByUser(auctionContainer, bidContainer, userId);
        if (auctionIdsResult.isError())
            return Result.err(auctionIdsResult);

        var auctionIds = auctionIdsResult.value();
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                Redis.setUserAuctions(jedis, userId, auctionIds);
            }
        }

        return Result.ok(auctionIdsToDaos(config, jedisPool, auctionContainer, auctionIds));
    }

    public static Result<List<AuctionDAO>, ServiceError> listAuctionsAboutToClose(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var auctionIds = Redis.getAuctionsAboutToClose(jedis);
                if (auctionIds != null)
                    return Result.ok(auctionIdsToDaos(config, jedisPool, auctionContainer, auctionIds));
                return Result.ok(new ArrayList<>());
            }
        } else {
            return Cosmos.listAuctionsAboutToClose(auctionContainer, AzureLogic.MAX_ABOUT_TO_CLOSE_AUCTIONS);
        }
    }

    public static Result<List<AuctionDAO>, ServiceError> listRecentAuctions(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var auctionIds = Redis.getRecentAuctions(jedis);
                if (auctionIds != null)
                    return Result.ok(auctionIdsToDaos(config, jedisPool, auctionContainer, auctionIds));
                return Result.ok(new ArrayList<>());
            }
        } else {
            // TODO: Implement
            return Result.ok(new ArrayList<>());
        }
    }

    public static Result<List<AuctionDAO>, ServiceError> listPopularAuctions(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var auctionIds = Redis.getPopularAuctions(jedis);
                if (auctionIds != null)
                    return Result.ok(auctionIdsToDaos(config, jedisPool, auctionContainer, auctionIds));
                return Result.ok(new ArrayList<>());
            }
        } else {
            // This feature requires redis
            return Result.ok(new ArrayList<>());
        }
    }

    public static void invalidateAuction(AzureConfig config, JedisPool jedisPool, String auctionId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                Redis.removeAuction(jedis, auctionId);
            }
        }
    }

    /* ------------------------- Auction Item ------------------------- */

    public static Result<AuctionItem, ServiceError> auctionDaoToItem(
            AuctionDAO auctionDao,
            UserDAO userDao,
            Optional<BidDAO> highestBidDao) {
        var userId = userDao.getId();
        if (userDao.getStatus() != UserDAO.Status.ACTIVE)
            userId = AzureLogic.DELETED_USER_ID;

        var highestBidItem = highestBidDao.map(b -> bidDaoToItem(b, userDao));
        var imageId = Optional.ofNullable(auctionDao.getPictureId()).map(Azure::mediaIdFromString);

        var auctionItem = new AuctionItem(
                auctionDao.getId(),
                auctionDao.getTitle(),
                auctionDao.getDescription(),
                userId,
                Azure.parseDateTime(auctionDao.getEndTime()),
                imageId,
                auctionDao.getStartingPrice(),
                statusToAuctionStatus(auctionDao.getStatus()),
                highestBidItem);

        return Result.ok(auctionItem);
    }

    public static Result<AuctionItem, ServiceError> auctionDaoToItem(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer bidContainer,
            CosmosContainer userContainer,
            AuctionDAO auctionDao) {
        var userDaoResult = getUser(config, jedisPool, userContainer, auctionDao.getUserId());
        if (userDaoResult.isError())
            return Result.err(userDaoResult);
        var userDao = userDaoResult.value();

        Optional<BidDAO> bidDao = Optional.empty();
        if (auctionDao.getWinnerBidId() != null) {
            var bidResult = getBid(config, jedisPool, bidContainer, auctionDao.getWinnerBidId());
            if (bidResult.isError())
                return Result.err(bidResult);
            bidDao = Optional.of(bidResult.value());
        }

        return auctionDaoToItem(auctionDao, userDao, bidDao);
    }

    public static Result<List<AuctionItem>, ServiceError> auctionDaosToItems(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer,
            CosmosContainer bidContainer,
            CosmosContainer userContainer,
            List<AuctionDAO> auctionDaos) {
        var auctionItems = new ArrayList<AuctionItem>(auctionDaos.size());
        for (var auctionDao : auctionDaos) {
            var auctionItemResult = auctionDaoToItem(config, jedisPool, bidContainer, userContainer, auctionDao);
            if (auctionItemResult.isError())
                return Result.err(auctionItemResult);
            auctionItems.add(auctionItemResult.value());
        }
        return Result.ok(auctionItems);
    }

    /* ------------------------- Bid DAO ------------------------- */

    public static void setBid(AzureConfig config, JedisPool jedisPool, BidDAO bidDao) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                Redis.setBid(jedis, bidDao);
            }
        }
    }

    public static Result<BidDAO, ServiceError> getBid(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer bidContainer,
            String bidId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var bidDao = Redis.getBid(jedis, bidId);
                if (bidDao != null)
                    return Result.ok(bidDao);
            }
        }

        var result = Cosmos.getBid(bidContainer, bidId);
        if (result.isEmpty())
            return Result.err(ServiceError.AUCTION_NOT_FOUND);

        var bidDao = result.get();
        setBid(config, jedisPool, bidDao);

        return Result.ok(bidDao);
    }

    public static Result<List<BidDAO>, ServiceError> listAuctionBids(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer bidContainer,
            String auctionId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var bids = Redis.getAuctionBids(jedis, auctionId);
                if (bids != null) {
                    var bidDaos = bidIdsToDaos(
                            config,
                            jedisPool,
                            bidContainer,
                            bids);
                    return Result.ok(bidDaos);
                }

                var result = Cosmos.listBidsOfAuction(bidContainer, auctionId);
                if (result.isError())
                    return result;

                Redis.setAuctionBids(
                        jedis,
                        auctionId,
                        result.value().stream().map(BidDAO::getId).toList());

                return result;
            }
        } else {
            return Cosmos.listBidsOfAuction(bidContainer, auctionId);
        }
    }

    /* ------------------------- Bid Item ------------------------- */

    public static BidItem bidDaoToItem(BidDAO bidDao, UserDAO userDao) {
        String userId = userDao.getId();
        if (userDao.getStatus() != UserDAO.Status.ACTIVE)
            userId = AzureLogic.DELETED_USER_ID;

        return new BidItem(
                bidDao.getId(),
                bidDao.getAuctionId(),
                userId,
                Azure.parseDateTime(bidDao.getTime()),
                bidDao.getAmount());
    }

    public static Result<BidItem, ServiceError> bidDaoToItem(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer bidContainer,
            CosmosContainer userContainer,
            BidDAO bidDao) {
        var userDaoResult = getUser(config, jedisPool, userContainer, bidDao.getUserId());
        if (userDaoResult.isError())
            return Result.err(userDaoResult);
        var userDao = userDaoResult.value();
        return Result.ok(bidDaoToItem(bidDao, userDao));
    }

    public static Result<List<BidItem>, ServiceError> bidDaosToItems(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer bidContainer,
            CosmosContainer userContainer,
            List<BidDAO> bidDaos) {
        var bidItems = new ArrayList<BidItem>(bidDaos.size());
        for (var bidDao : bidDaos) {
            var bidItemResult = bidDaoToItem(config, jedisPool, bidContainer, userContainer, bidDao);
            if (bidItemResult.isError())
                return Result.err(bidItemResult);
            bidItems.add(bidItemResult.value());
        }
        return Result.ok(bidItems);
    }

    /* ------------------------- User DAO ------------------------- */

    public static void setUser(AzureConfig config, JedisPool jedisPool, UserDAO userDao) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                Redis.setUser(jedis, userDao);
            }
        }
    }

    public static Result<UserDAO, ServiceError> getUser(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer userContainer,
            String userId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var user = Redis.getUser(jedis, userId);
                if (user != null)
                    return Result.ok(user);
            }
        }

        var userResult = Cosmos.getUser(userContainer, userId);
        if (userResult.isEmpty())
            return Result.err(ServiceError.USER_NOT_FOUND);

        var userDao = userResult.get();
        setUser(config, jedisPool, userDao);

        return Result.ok(userDao);
    }

    /* ------------------------- User DAO ------------------------- */

    public static UserItem userDaoToItem(UserDAO userDao) {
        if (userDao.getStatus() == UserDAO.Status.ACTIVE) {
            var photoId = Optional.ofNullable(userDao.getPhotoId()).map(Azure::mediaIdFromString);
            return new UserItem(
                    userDao.getId(),
                    userDao.getName(),
                    photoId);
        } else {
            return new UserItem(AzureLogic.DELETED_USER_ID, AzureLogic.DELETED_USER_NAME, Optional.empty());
        }
    }

    /* ------------------------- Question DAO ------------------------- */

    public static void setQuestion(AzureConfig config, JedisPool jedisPool, QuestionDAO questionDao) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                Redis.setQuestion(jedis, questionDao);
            }
        }
    }

    public static Result<QuestionDAO, ServiceError> getQuestion(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer questionContainer,
            String questionId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var question = Redis.getQuestion(jedis, questionId);
                if (question != null)
                    return Result.ok(question);
            }
        }

        var questionResult = Cosmos.getQuestion(questionContainer, questionId);
        if (questionResult.isEmpty())
            return Result.err(ServiceError.QUESTION_NOT_FOUND);

        var questionDao = questionResult.get();
        setQuestion(config, jedisPool, questionDao);

        return Result.ok(questionDao);
    }

    public static Result<List<QuestionDAO>, ServiceError> listAuctionQuestions(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer questionContainer,
            String auctionId) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var questions = Redis.getAuctionQuestions(jedis, auctionId);
                if (questions != null) {
                    var questionDaos = questionIdsToDaos(
                            config,
                            jedisPool,
                            questionContainer,
                            questions);
                    return Result.ok(questionDaos);
                }

                var result = Cosmos.listQuestionsOfAuction(questionContainer, auctionId);
                if (result.isError())
                    return result;

                Redis.setAuctionQuestions(
                        jedis,
                        auctionId,
                        result.value().stream().map(QuestionDAO::getId).toList());

                return result;
            }
        } else {
            return Cosmos.listQuestionsOfAuction(questionContainer, auctionId);
        }
    }

    public static Result<List<AuctionDAO>, ServiceError> listUserAuctions(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer,
            String userId,
            boolean open) {
        if (config.isCachingEnabled()) {
            try (var jedis = jedisPool.getResource()) {
                var auctions = Redis.getUserAuctions(jedis, userId);
                if (auctions != null) {
                    var auctionDaos = auctionIdsToDaos(
                            config,
                            jedisPool,
                            auctionContainer,
                            auctions);
                    return Result.ok(auctionDaos);
                }

                var result = Cosmos.listAuctionsOfUser(auctionContainer, userId, false);
                if (result.isError())
                    return result;

                Redis.setUserAuctions(
                        jedis,
                        userId,
                        result.value().stream().map(AuctionDAO::getId).toList());

                return result;
            }
        } else {
            return Cosmos.listAuctionsOfUser(auctionContainer, userId, open);
        }
    }

    /* ------------------------- Question Item ------------------------- */

    public static Result<QuestionItem, ServiceError> questionDaoToItem(
            QuestionDAO questionDao,
            UserDAO userDao) {
        String userId = userDao.getId();
        if (userDao.getStatus() != UserDAO.Status.ACTIVE)
            userId = AzureLogic.DELETED_USER_ID;

        Optional<ReplyItem> reply = Optional.empty();
        if (questionDao.getReply() != null) {
            var replyDao = questionDao.getReply();
            if (!replyDao.getUserId().equals(questionDao.getUserId()))
                throw new IllegalStateException("Reply user id does not match question user id");

            reply = Optional.of(new ReplyItem(
                    questionDao.getId(),
                    userId,
                    replyDao.getReply()));
        }

        var questionItem = new QuestionItem(
                questionDao.getId(),
                questionDao.getAuctionId(),
                userId,
                questionDao.getQuestion(),
                reply);

        return Result.ok(questionItem);
    }

    public static Result<List<QuestionItem>, ServiceError> questionDaosToItems(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer userContainer,
            CosmosContainer questionContainer,
            List<QuestionDAO> questionDaos) {
        var questionItems = new ArrayList<QuestionItem>();
        for (var questionDao : questionDaos) {
            var userResult = getUser(config, jedisPool, userContainer, questionDao.getUserId());
            if (userResult.isError())
                return Result.err(userResult.error());

            var questionItemResult = questionDaoToItem(questionDao, userResult.value());
            if (questionItemResult.isError())
                return Result.err(questionItemResult.error());

            questionItems.add(questionItemResult.value());
        }
        return Result.ok(questionItems);
    }

    /* ------------------------- Mixed Ops ------------------------- */

    public static AuctionStatus statusToAuctionStatus(AuctionDAO.Status status) {
        return switch (status) {
            case OPEN -> AuctionStatus.OPEN;
            case CLOSED -> AuctionStatus.CLOSED;
            case DELETED -> throw new IllegalArgumentException("Cannot convert deleted status to auction status");
        };
    }

    /* ------------------------- Internal ------------------------- */

    private static List<AuctionDAO> auctionIdsToDaos(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer auctionContainer,
            List<String> auctionIds) {
        var auctions = new ArrayList<AuctionDAO>(auctionIds.size());
        for (var bidId : auctionIds) {
            var bidResult = getAuction(config, jedisPool, auctionContainer, bidId);
            if (bidResult.isError())
                continue;

            auctions.add(bidResult.value());
        }
        return auctions;
    }

    // Ignores errors
    private static List<BidDAO> bidIdsToDaos(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer bidContainer,
            List<String> bidIds) {
        var bids = new ArrayList<BidDAO>(bidIds.size());
        for (var bidId : bidIds) {
            var bidResult = getBid(config, jedisPool, bidContainer, bidId);
            if (bidResult.isError())
                continue;

            bids.add(bidResult.value());
        }
        return bids;
    }

    // Ignores errors
    private static List<QuestionDAO> questionIdsToDaos(
            AzureConfig config,
            JedisPool jedisPool,
            CosmosContainer questionContainer,
            List<String> questionIds) {
        var questions = new ArrayList<QuestionDAO>(questionIds.size());
        for (var questionId : questionIds) {
            var questionResult = getQuestion(config, jedisPool, questionContainer, questionId);
            if (questionResult.isError())
                continue;

            questions.add(questionResult.value());
        }
        return questions;
    }
}
