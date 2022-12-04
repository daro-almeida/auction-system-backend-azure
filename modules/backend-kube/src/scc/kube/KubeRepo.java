package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import redis.clients.jedis.Jedis;
import scc.PagingWindow;
import scc.UserService.CreateUserParams;
import scc.exception.AuctionNotFoundException;
import scc.exception.BidConflictException;
import scc.exception.BidNotFoundException;
import scc.exception.QuestionAlreadyRepliedException;
import scc.exception.QuestionNotFoundException;
import scc.exception.UserAlreadyExistsException;
import scc.exception.UserNotFoundException;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.QuestionDao;
import scc.kube.dao.UserDao;

public class KubeRepo implements AutoCloseable {
    private final Jedis jedis;
    private final Mongo mongo;

    public KubeRepo(Jedis jedis, Mongo mongo) {
        this.jedis = jedis;
        this.mongo = mongo;
    }

    /* ------------------------- User ------------------------- */

    @WithSpan
    public UserDao createUser(CreateUserParams params) throws UserAlreadyExistsException {
        var userDao = new UserDao();
        userDao.username = params.username();
        userDao.name = params.name();
        userDao.hashedPassword = Kube.hashUserPassword(params.password());
        userDao.status = UserDao.Status.ACTIVE;
        userDao.createTime = LocalDateTime.now(ZoneOffset.UTC);

        userDao = this.mongo.createUser(userDao);
        Redis.setUser(this.jedis, userDao);

        return userDao;
    }

    public ObjectId getUserIdFromUsername(String username) throws UserNotFoundException {
        var userId = Redis.getUserIdFromUsername(this.jedis, username);
        if (userId == null) {
            var userDao = this.mongo.getUserByUsername(username);
            Redis.setUser(this.jedis, userDao);
            userId = userDao.id;
        }
        return userId;
    }

    @WithSpan
    public UserDao getUser(ObjectId userId) throws UserNotFoundException {
        var userDao = Redis.getUser(this.jedis, userId);
        if (userDao == null) {
            userDao = this.mongo.getUser(userId);
            Redis.setUser(this.jedis, userDao);
        }
        return userDao;
    }

    @WithSpan
    public Map<ObjectId, UserDao> getUserMany(Iterable<ObjectId> userIds) throws UserNotFoundException {
        var userIdsSet = new HashSet<ObjectId>();
        userIds.forEach(userIdsSet::add);

        var userDaoMap = Redis.getUserMany(this.jedis, userIdsSet);
        var userDaoIdsSet = new HashSet<ObjectId>();
        userDaoMap.values().forEach(userDao -> userDaoIdsSet.add(userDao.id));

        var userDaoIdsSetDiff = new HashSet<ObjectId>(userIdsSet);
        userDaoIdsSetDiff.removeAll(userDaoIdsSet);

        if (!userDaoIdsSetDiff.isEmpty()) {
            var userDaoMap2 = this.mongo.getUserMany(userDaoIdsSetDiff);
            userDaoMap.putAll(userDaoMap2);
            Redis.setUserMany(this.jedis, userDaoMap2.values());
        }

        return userDaoMap;
    }

    @WithSpan
    public UserDao updateUser(ObjectId userId, UserDao userDao) throws UserNotFoundException {
        var updatedDao = this.mongo.updateUser(userId, userDao);
        Redis.setUser(this.jedis, updatedDao);
        return updatedDao;
    }

    @WithSpan
    public UserDao deactivateUser(ObjectId userId) throws UserNotFoundException {
        var userDao = this.mongo.deactivateUser(userId);
        Redis.setUser(this.jedis, userDao);
        return userDao;
    }

    @WithSpan
    public List<AuctionDao> getUserAuctions(ObjectId userId) {
        var auctionIds = Redis.getUserAuctions(this.jedis, userId);
        if (auctionIds != null)
            return List.copyOf(this.getAuctionMany(auctionIds).values());

        var auctionDaos = this.mongo.getUserAuctions(userId);
        Redis.setAuctionMany(jedis, auctionDaos);
        Redis.setUserAuctions(this.jedis, userId, auctionDaos.stream().map(a -> a.id).toList());

        return auctionDaos;
    }

    @WithSpan
    public List<AuctionDao> getAuctionsFollowedByUser(ObjectId userId) throws BidNotFoundException {
        // Redis either stores all user followed auctions or none of them
        var auctionIds = Redis.getUserFollowedAuctions(this.jedis, userId);
        if (!auctionIds.isEmpty())
            return List.copyOf(this.getAuctionMany(auctionIds).values());

        var auctionDaos = this.mongo.getAuctionsFollowedByUser(userId);
        Redis.setAuctionMany(jedis, auctionDaos);
        Redis.addUserFollowedAuctionMany(this.jedis, userId, auctionDaos.stream().map(a -> a.id).toList());

        return auctionDaos;
    }

    @WithSpan
    public Map<ObjectId, String> getUserDisplayNameMany(Iterable<ObjectId> userIds) throws UserNotFoundException {
        var userIdsList = new ArrayList<ObjectId>();
        userIds.forEach(userIdsList::add);

        var displayNames = Redis.getUserDisplayNameMany(this.jedis, userIdsList);
        if (displayNames.size() == userIdsList.size())
            return displayNames;

        var missingUserIds = new HashSet<ObjectId>();
        for (var userId : userIdsList) {
            if (!displayNames.containsKey(userId))
                missingUserIds.add(userId);
        }

        var missingUserDaos = this.mongo.getUserMany(missingUserIds);
        var missingUserDisplayNames = new HashMap<ObjectId, String>();
        for (var userDao : missingUserDaos.values())
            missingUserDisplayNames.put(userDao.id, Kube.userDisplayNameFromDao(userDao));
        Redis.setUserDisplayNameMany(this.jedis, missingUserDisplayNames);

        displayNames.putAll(missingUserDisplayNames);
        return displayNames;
    }

    /* ------------------------- Auction ------------------------- */

    @WithSpan
    public AuctionDao getAuction(ObjectId auctionId) throws AuctionNotFoundException {
        var auctionDao = Redis.getAuction(this.jedis, auctionId);
        if (auctionDao != null)
            return auctionDao;
        auctionDao = this.mongo.getAuction(auctionId);
        Redis.setAuction(this.jedis, auctionDao);
        return auctionDao;
    }

    @WithSpan
    public Map<ObjectId, AuctionDao> getAuctionMany(List<ObjectId> auctionIds) {
        var auctionDaos = new HashMap<ObjectId, AuctionDao>();
        var missingAuctionIds = new HashSet<>(auctionIds);
        var auctionDaoMap = Redis.getAuctionMany(this.jedis, auctionIds);
        auctionDaos.putAll(auctionDaoMap);
        for (var auctionId : auctionDaoMap.keySet())
            missingAuctionIds.remove(auctionId);

        if (missingAuctionIds.isEmpty())
            return auctionDaos;

        var missingAuctionDaos = this.mongo.getAuctionMany(new ArrayList<>(missingAuctionIds));
        auctionDaos.putAll(missingAuctionDaos);
        Redis.setAuctionMany(this.jedis, new ArrayList<>(missingAuctionDaos.values()));

        return auctionDaos;
    }

    @WithSpan
    public AuctionDao createAuction(AuctionDao auctionDao) {
        auctionDao = this.mongo.createAuction(auctionDao);
        Redis.setAuction(this.jedis, auctionDao);
        Redis.pushUserAuction(jedis, auctionDao.userId, auctionDao.id);
        Redis.pushRecentAuction(jedis, auctionDao.id);
        return auctionDao;
    }

    @WithSpan
    public AuctionDao updateAuction(ObjectId auctionId, AuctionDao auctionDao) throws AuctionNotFoundException {
        var updatedDao = this.mongo.updateAuction(auctionId, auctionDao);
        Redis.setAuction(this.jedis, updatedDao);
        return updatedDao;
    }

    public AuctionDao closeAuction(ObjectId auctionId) throws AuctionNotFoundException {
        var auctionDao = this.mongo.closeAuction(auctionId);
        Redis.setAuction(this.jedis, auctionDao);
        return auctionDao;
    }

    @WithSpan
    public List<BidDao> getAuctionBids(ObjectId auctionId, PagingWindow window) {
        // TODO: this could be cached
        return this.mongo.getAuctionBids(auctionId, window);
    }

    @WithSpan
    public Map<ObjectId, BidDao> getAuctionTopBidMany(Collection<ObjectId> auctionIds) throws BidNotFoundException {
        var auctionIdsSet = new HashSet<>(auctionIds);
        var cachedTopBidIds = Redis.getAuctionTopBidMany(this.jedis, auctionIds);
        var cachedTopBids = this.getBidMany(cachedTopBidIds.values());

        for (var bidDao : cachedTopBids.values())
            auctionIdsSet.remove(bidDao.auctionId);

        var missingTopBids = this.mongo.getAuctionTopBidMany(auctionIdsSet);
        for (var entry : missingTopBids.entrySet())
            Redis.setAuctionTopBid(this.jedis, entry.getKey(), entry.getValue().id);
        Redis.setBidMany(this.jedis, new ArrayList<>(missingTopBids.values()));

        var topBids = new HashMap<ObjectId, BidDao>();
        for (var auctionId : auctionIds) {
            if (cachedTopBidIds.containsKey(auctionId))
                topBids.put(auctionId, cachedTopBids.get(cachedTopBidIds.get(auctionId)));
            else if (missingTopBids.containsKey(auctionId))
                topBids.put(auctionId, missingTopBids.get(auctionId));
        }

        return topBids;
    }

    @WithSpan
    public List<QuestionDao> getAuctionQuestions(ObjectId auctionId, PagingWindow window) {
        // TODO: this could be cached
        return this.mongo.getAuctionQuestions(auctionId, window);
    }

    @WithSpan
    public List<AuctionDao> getAuctionsSoonToClose() {
        var auctionIds = Redis.getSoonToCloseAuctionIds(this.jedis);
        var auctionDaos = this.getAuctionMany(auctionIds);
        return List.copyOf(auctionDaos.values());
    }

    @WithSpan
    public List<AuctionDao> getRecentAuctions() {
        var auctionIds = Redis.getRecentAuctionIds(this.jedis);
        var auctionDaos = this.getAuctionMany(auctionIds);
        return List.copyOf(auctionDaos.values());
    }

    @WithSpan
    public List<AuctionDao> getPopularAuctions() {
        var auctionIds = Redis.getPopularAuctions(this.jedis);
        var auctionDaos = this.getAuctionMany(auctionIds);
        return List.copyOf(auctionDaos.values());
    }

    /* ------------------------- Bid ------------------------- */

    @WithSpan
    public BidDao createBid(ObjectId auctionId, BidDao bidDao) throws BidConflictException {
        bidDao = this.mongo.createBid(auctionId, bidDao);
        Redis.setBid(this.jedis, bidDao);
        Redis.setAuctionTopBid(this.jedis, auctionId, bidDao.id);
        return bidDao;
    }

    @WithSpan
    public Map<ObjectId, BidDao> getBidMany(Collection<ObjectId> bidIds) throws BidNotFoundException {
        var bidDaos = Redis.getBidMany(this.jedis, bidIds);
        if (bidDaos.size() == bidIds.size())
            return bidDaos;

        var missingBidIds = new HashSet<ObjectId>(bidIds);
        missingBidIds.removeAll(bidDaos.keySet());

        var missingBidDaos = this.mongo.getBidMany(missingBidIds);
        bidDaos.putAll(missingBidDaos);

        return bidDaos;
    }

    /* ------------------------- Question ------------------------- */

    @WithSpan
    public QuestionDao createQuestion(QuestionDao questionDao) {
        questionDao = this.mongo.createQuestion(questionDao);
        Redis.setQuestion(this.jedis, questionDao);
        return questionDao;
    }

    @WithSpan
    public QuestionDao createReply(ObjectId questionId, QuestionDao.Reply reply)
            throws QuestionNotFoundException, QuestionAlreadyRepliedException {
        var questionDao = this.mongo.createReply(questionId, reply);
        Redis.setQuestion(this.jedis, questionDao);
        return questionDao;
    }

    @Override
    public void close() throws Exception {
    }

    /* ------------------------- Internal ------------------------- */

}
