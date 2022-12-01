package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.bson.types.ObjectId;

import redis.clients.jedis.JedisPool;
import scc.AuctionService;
import scc.PagingWindow;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateAuctionOps;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.kube.config.KubeConfig;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.QuestionDao;

public class KubeAuctionService implements AuctionService {
    private static final Logger logger = Logger.getLogger(KubeAuctionService.class.getName());

    private final KubeConfig config;
    private final JedisPool jedisPool;
    private final Mongo mongo;
    private final Rabbitmq rabbitmq;

    public KubeAuctionService(KubeConfig config, JedisPool jedisPool, Mongo mongo, Rabbitmq rabbitmq) {
        this.config = config;
        this.jedisPool = jedisPool;
        this.mongo = mongo;
        this.rabbitmq = rabbitmq;
    }

    @Override
    public Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params) {
        if (params.title().isBlank() || params.description().isBlank() || params.startingPrice() <= 0)
            return Result.err(ServiceError.BAD_REQUEST);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var authResult = data.validate(token);
            if (authResult.isError())
                return Result.err(authResult);

            var userId = authResult.value();
            var userDaoResult = data.getUser(userId);
            if (userDaoResult.isError())
                return Result.err(userDaoResult);
            var userDao = userDaoResult.value();

            if (!userDao.username.equals(params.owner()))
                return Result.err(ServiceError.UNAUTHORIZED);

            var auctionDao = new AuctionDao();
            auctionDao.title = params.title();
            auctionDao.description = params.description();
            auctionDao.userId = userId;
            auctionDao.userIdDisplay = userDao.username;
            auctionDao.createTime = LocalDateTime.now(ZoneOffset.UTC);
            auctionDao.closeTime = params.endTime();
            auctionDao.initialPrice = params.startingPrice();
            auctionDao.status = AuctionDao.Status.OPEN;

            var createResult = data.createAuction(auctionDao);
            if (createResult.isError())
                return Result.err(createResult);
            auctionDao = createResult.value();

            this.rabbitmq.broadcastCreatedAuction(auctionDao.id);
            var auctionItem = data.auctionDaoToItem(auctionDao, Optional.empty());

            return Result.ok(auctionItem);
        }
    }

    @Override
    public Result<AuctionItem, ServiceError> getAuction(String auctionIdStr) {
        if (!ObjectId.isValid(auctionIdStr))
            return Result.err(ServiceError.BAD_REQUEST);
        var auctionId = new ObjectId(auctionIdStr);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var auctionDaoResult = data.getAuction(auctionId);
            if (auctionDaoResult.isError())
                return Result.err(auctionDaoResult);
            var auctionDao = auctionDaoResult.value();

            var auctionItem = data.auctionDaoToItem(auctionDao);
            return Result.ok(auctionItem);
        }
    }

    @Override
    public Result<Void, ServiceError> updateAuction(SessionToken token, String auctionIdStr, UpdateAuctionOps ops) {
        if (!ObjectId.isValid(auctionIdStr))
            return Result.err(ServiceError.BAD_REQUEST);
        var auctionId = new ObjectId(auctionIdStr);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var authResult = data.validate(token);
            if (authResult.isError())
                return Result.err(authResult);
            var userId = authResult.value();

            var auctionDaoResult = data.getAuction(auctionId);
            if (auctionDaoResult.isError())
                return Result.err(auctionDaoResult);
            var auctionDao = auctionDaoResult.value();

            if (!auctionDao.userId.equals(userId))
                return Result.err(ServiceError.UNAUTHORIZED);

            var updateDao = new AuctionDao();
            if (ops.shouldUpdateTitle())
                updateDao.title = ops.getTitle();
            if (ops.shouldUpdateDescription())
                updateDao.description = ops.getDescription();
            if (ops.shouldUpdateImage())
                updateDao.imageId = Kube.mediaIdToString(ops.getImage());

            var updateResult = data.updateAuction(auctionId, updateDao);
            if (updateResult.isError())
                return Result.err(updateResult);

            return Result.ok();
        }
    }

    @Override
    public Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params) {
        if (!ObjectId.isValid(params.auctionId()) || params.price() <= 0)
            return Result.err(ServiceError.BAD_REQUEST);
        var auctionId = new ObjectId(params.auctionId());

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var authResult = data.validate(token);
            if (authResult.isError()) {
                logger.info("createBid: authResult.isError()");
                return Result.err(authResult);
            }
            var userId = authResult.value();
            var userDao = data.getUser(userId).value();

            var bidDao = new BidDao();
            bidDao.userId = userId;
            bidDao.userIdDisplay = userDao.username;
            bidDao.amount = params.price();
            bidDao.createTime = LocalDateTime.now(ZoneOffset.UTC);

            var createResult = data.createBid(auctionId, bidDao);
            if (createResult.isError())
                return Result.err(createResult);
            bidDao = createResult.value();
            this.rabbitmq.broadcastCreatedBid(auctionId, bidDao.id);

            var bidItem = data.bidDaoToItem(auctionId, bidDao);
            return Result.ok(bidItem);
        }
    }

    @Override
    public Result<List<BidItem>, ServiceError> listAuctionBids(String auctionIdStr, PagingWindow window) {
        if (!ObjectId.isValid(auctionIdStr))
            return Result.err(ServiceError.BAD_REQUEST);
        var auctionId = new ObjectId(auctionIdStr);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var result = data.getAuctionBids(auctionId, window);
            if (result.isError())
                return Result.err(result);

            var bidDaos = result.value();
            var bidItems = bidDaos.stream().map(b -> data.bidDaoToItem(auctionId, b)).toList();
            return Result.ok(bidItems);
        }
    }

    @Override
    public Result<QuestionItem, ServiceError> createQuestion(SessionToken token, CreateQuestionParams params) {
        if (!ObjectId.isValid(params.auctionId()))
            return Result.err(ServiceError.BAD_REQUEST);
        var auctionId = new ObjectId(params.auctionId());

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var authResult = data.validate(token);
            if (authResult.isError())
                return Result.err(authResult);
            var userId = authResult.value();
            var userDao = data.getUser(userId).value();

            var questionDao = new QuestionDao();
            questionDao.auctionId = auctionId;
            questionDao.userId = userId;
            questionDao.userIdDisplay = userDao.username;
            questionDao.question = params.question();
            questionDao.createTime = LocalDateTime.now(ZoneOffset.UTC);

            var createResult = data.createQuestion(questionDao);
            if (createResult.isError())
                return Result.err(createResult);
            questionDao = createResult.value();

            var questionItem = data.questionDaoToItem(questionDao);
            return Result.ok(questionItem);
        }
    }

    @Override
    public Result<ReplyItem, ServiceError> createReply(SessionToken token, CreateReplyParams params) {
        if (!ObjectId.isValid(params.auctionId()) || !ObjectId.isValid(params.questionId()) || params.reply().isBlank())
            return Result.err(ServiceError.BAD_REQUEST);
        var auctionId = new ObjectId(params.auctionId());
        var questionId = new ObjectId(params.questionId());

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);

            var authResult = data.validate(token);
            if (authResult.isError())
                return Result.err(authResult);
            var userId = authResult.value();

            var auctionDaoResult = data.getAuction(auctionId);
            if (auctionDaoResult.isError())
                return Result.err(auctionDaoResult);
            var auctionDao = auctionDaoResult.value();

            if (!auctionDao.userId.equals(userId))
                return Result.err(ServiceError.UNAUTHORIZED);

            var ownerDaoResult = data.getUser(auctionDao.userId);
            if (ownerDaoResult.isError())
                return Result.err(ownerDaoResult);
            var ownerDao = ownerDaoResult.value();

            var replyDao = new QuestionDao.Reply();
            replyDao.reply = params.reply();
            replyDao.createTime = LocalDateTime.now(ZoneOffset.UTC);
            replyDao.userIdDisplay = ownerDao.username;

            var createResult = data.createReply(questionId, replyDao);
            if (createResult.isError())
                return Result.err(createResult);
            var questionDao = createResult.value();

            var replyItem = data.questionDaoToReplyItem(questionDao);
            return Result.ok(replyItem);
        }
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> listAuctionQuestions(String auctionIdStr, PagingWindow window) {
        if (!ObjectId.isValid(auctionIdStr))
            return Result.err(ServiceError.BAD_REQUEST);
        var auctionId = new ObjectId(auctionIdStr);

        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var result = data.getAuctionQuestions(auctionId, window);
            if (result.isError())
                return Result.err(result);

            var questionDaos = result.value();
            var questionItems = questionDaos.stream().map(data::questionDaoToItem).toList();
            return Result.ok(questionItems);
        }
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listUserAuctions(String username, boolean open) {
        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);

            var userDaoResult = data.getUserByUsername(username);
            if (userDaoResult.isError())
                return Result.err(userDaoResult);
            var userDao = userDaoResult.value();
            var userId = userDao.id;

            var result = data.getUserAuctions(userId);
            if (result.isError())
                return Result.err(result);

            var auctionDaos = result.value();
            var auctionItems = data.auctionDaoToItemMany(auctionDaos
                    .stream()
                    .filter(a -> a.status.equals(AuctionDao.Status.OPEN) || !open)
                    .toList())
                    .values().stream().toList();

            return Result.ok(auctionItems);
        }
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsFollowedByUser(String username) {
        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var userDaoResult = data.getUserByUsername(username);
            if (userDaoResult.isError())
                return Result.err(userDaoResult);
            var userId = userDaoResult.value().id;
            var result = data.getAuctionsFollowedByUser(userId);
            if (result.isError())
                return Result.err(result);

            var auctionDaos = result.value();
            var auctionItems = data.auctionDaoToItemMany(auctionDaos).values().stream().toList();
            return Result.ok(auctionItems);
        }
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose() {
        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var result = data.getAuctionsSoonToClose();
            if (result.isError())
                return Result.err(result);

            var auctionDaos = result.value();
            var auctionItems = data.auctionDaoToItemMany(auctionDaos).values().stream().toList();
            return Result.ok(auctionItems);
        }
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listRecentAuctions() {
        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var result = data.getRecentAuctions();
            if (result.isError())
                return Result.err(result);

            var auctionDaos = result.value();
            var auctionItems = data.auctionDaoToItemMany(auctionDaos).values().stream().toList();
            return Result.ok(auctionItems);
        }
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listPopularAuctions() {
        try (var jedis = this.jedisPool.getResource()) {
            var data = new KubeData(this.config, this.mongo, jedis);
            var result = data.getPopularAuctions();
            if (result.isError())
                return Result.err(result);

            var auctionDaos = result.value();
            var auctionItems = data.auctionDaoToItemMany(auctionDaos).values().stream().toList();
            return Result.ok(auctionItems);
        }
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> queryAuctions(String query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> queryQuestionsFromAuction(String auctionId, String query) {
        // TODO Auto-generated method stub
        return null;
    }

}
