package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import scc.AuctionService;
import scc.AuctionStatus;
import scc.PagingWindow;
import scc.SessionToken;
import scc.UpdateAuctionOps;
import scc.exception.BadRequestException;
import scc.exception.BidNotFoundException;
import scc.exception.ServiceException;
import scc.exception.UnauthorizedException;
import scc.exception.UserNotFoundException;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.QuestionDao;

public class KubeAuctionService implements AuctionService {
    private static final Logger logger = Logger.getLogger(KubeAuctionService.class.getName());

    private final Auth auth;
    private final KubeRepo repo;
    private final Rabbitmq rabbitmq;

    public KubeAuctionService(Auth auth, KubeRepo repo, Rabbitmq rabbitmq) {
        this.auth = auth;
        this.repo = repo;
        this.rabbitmq = rabbitmq;
    }

    @Override
    @WithSpan
    public AuctionItem createAuction(SessionToken token, CreateAuctionParams params) throws ServiceException {
        if (params.title().isBlank() || params.description().isBlank() || params.startingPrice() <= 0)
            throw new BadRequestException();

        logger.fine("Validating user token");
        this.auth.validate(token, params.owner());
        var userId = this.repo.getUserIdFromUsername(params.owner());

        var auctionDao = new AuctionDao();
        auctionDao.title = params.title();
        auctionDao.description = params.description();
        auctionDao.userId = userId;
        auctionDao.createTime = LocalDateTime.now(ZoneOffset.UTC);
        auctionDao.closeTime = params.endTime();
        auctionDao.initialPrice = params.startingPrice();
        auctionDao.status = AuctionDao.Status.OPEN;

        logger.fine("Creating auction: " + auctionDao);
        auctionDao = this.repo.createAuction(auctionDao);
        this.rabbitmq.broadcastCreatedAuction(auctionDao.id);

        var auctionItem = this.auctionDaoToItem(auctionDao);
        logger.fine("Returning auction item: " + auctionItem);
        return auctionItem;
    }

    @Override
    @WithSpan
    public AuctionItem getAuction(String auctionIdStr) throws ServiceException {
        var auctionId = this.auctionIdFromString(auctionIdStr);
        var auctionDao = this.repo.getAuction(auctionId);
        var auctionItem = this.auctionDaoToItem(auctionDao);
        return auctionItem;
    }

    @Override
    @WithSpan
    public void updateAuction(SessionToken token, String auctionIdStr, UpdateAuctionOps ops) throws ServiceException {
        var auctionId = this.auctionIdFromString(auctionIdStr);
        var username = this.auth.validate(token);
        var userId = this.repo.getUserIdFromUsername(username);
        var auctionDao = this.repo.getAuction(auctionId);

        if (auctionDao.userId.equals(userId))
            throw new UnauthorizedException("You are not the owner of this auction");

        var updateDao = new AuctionDao();
        if (ops.shouldUpdateTitle())
            updateDao.title = ops.getTitle();
        if (ops.shouldUpdateDescription())
            updateDao.description = ops.getDescription();
        if (ops.shouldUpdateImage())
            updateDao.imageId = Kube.mediaIdToString(ops.getImage());

        this.repo.updateAuction(auctionId, auctionDao);
        return;
    }

    @Override
    @WithSpan
    public BidItem createBid(SessionToken token, CreateBidParams params) throws ServiceException {
        if (!ObjectId.isValid(params.auctionId()) || params.price() <= 0)
            throw new BadRequestException();

        var username = this.auth.validate(token, params.username());
        var auctionId = this.auctionIdFromString(params.auctionId());
        var userId = this.repo.getUserIdFromUsername(username);

        var bidDao = new BidDao();
        bidDao.userId = userId;
        bidDao.amount = params.price();
        bidDao.createTime = LocalDateTime.now(ZoneOffset.UTC);

        bidDao = this.repo.createBid(auctionId, bidDao);
        this.rabbitmq.broadcastCreatedBid(auctionId, bidDao.id);
        var bidItem = this.bidDaoToItem(bidDao);
        return bidItem;
    }

    @Override
    @WithSpan
    public List<BidItem> listAuctionBids(String auctionIdStr, PagingWindow window) throws ServiceException {
        var auctionId = this.auctionIdFromString(auctionIdStr);
        var bidDaos = this.repo.getAuctionBids(auctionId, window);
        var bidItems = this.bidDaosToItems(bidDaos);
        return bidDaos.stream().map(b -> bidItems.get(b.id)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @WithSpan
    public QuestionItem createQuestion(SessionToken token, CreateQuestionParams params) throws ServiceException {
        var auctionId = this.auctionIdFromString(params.auctionId());
        var username = this.auth.validate(token);
        var userId = this.repo.getUserIdFromUsername(username);

        var questionDao = new QuestionDao();
        questionDao.auctionId = auctionId;
        questionDao.userId = userId;
        questionDao.question = params.question();
        questionDao.createTime = LocalDateTime.now(ZoneOffset.UTC);

        logger.fine("Creating question: " + questionDao);
        questionDao = this.repo.createQuestion(questionDao);
        var questionItem = this.questionDaoToItem(questionDao);
        return questionItem;
    }

    @Override
    @WithSpan
    public ReplyItem createReply(SessionToken token, CreateReplyParams params) throws ServiceException {
        if (params.reply().isBlank())
            throw new BadRequestException("Reply cannot be blank");

        var auctionId = this.auctionIdFromString(params.auctionId());
        var questionId = this.questionIdFromString(params.questionId());
        var username = this.auth.validate(token);
        var userId = this.repo.getUserIdFromUsername(username);
        var auctionDao = this.repo.getAuction(auctionId);

        if (!auctionDao.userId.equals(userId))
            throw new UnauthorizedException("You are not the owner of this auction");

        var replyDao = new QuestionDao.Reply();
        replyDao.reply = params.reply();
        replyDao.createTime = LocalDateTime.now(ZoneOffset.UTC);
        replyDao.userId = userId;

        var questionDao = this.repo.createReply(questionId, replyDao);
        var replyItem = this.questionDaoToReplyItem(questionDao);
        return replyItem;
    }

    @Override
    @WithSpan
    public List<QuestionItem> listAuctionQuestions(String auctionIdStr, PagingWindow window) throws ServiceException {
        var auctionId = this.auctionIdFromString(auctionIdStr);
        var questionDaos = this.repo.getAuctionQuestions(auctionId, window);
        var questionItems = this.questionDaosToItems(questionDaos);
        return questionItems;
    }

    @Override
    @WithSpan
    public List<AuctionItem> listUserAuctions(String username, boolean open) throws ServiceException {
        var userId = this.repo.getUserIdFromUsername(username);
        var auctionDaos = this.repo.getUserAuctions(userId)
                .stream().filter(a -> a.status == AuctionDao.Status.OPEN)
                .collect(Collectors.toCollection(ArrayList::new));
        var auctionItems = this.auctionDaosToItems(auctionDaos);
        return auctionDaos.stream().map(a -> auctionItems.get(a.id)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @WithSpan
    public List<AuctionItem> listAuctionsFollowedByUser(String username) throws ServiceException {
        var userId = this.repo.getUserIdFromUsername(username);
        var auctionDaos = this.repo.getAuctionsFollowedByUser(userId);
        var auctionItems = this.auctionDaosToItems(auctionDaos);
        return auctionDaos.stream().map(a -> auctionItems.get(a.id)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @WithSpan
    public List<AuctionItem> listAuctionsAboutToClose() throws ServiceException {
        var auctionDaos = this.repo.getAuctionsSoonToClose();
        var auctionItems = this.auctionDaosToItems(auctionDaos);
        return auctionDaos.stream().map(a -> auctionItems.get(a.id)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<AuctionItem> listRecentAuctions() throws ServiceException {
        var auctionDaos = this.repo.getRecentAuctions();
        var auctionItems = this.auctionDaosToItems(auctionDaos);
        return auctionDaos.stream().map(a -> auctionItems.get(a.id)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<AuctionItem> listPopularAuctions() throws ServiceException {
        var auctionDaos = this.repo.getPopularAuctions();
        var auctionItems = this.auctionDaosToItems(auctionDaos);
        return auctionDaos.stream().map(a -> auctionItems.get(a.id)).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void close() throws Exception {
    }

    private ObjectId auctionIdFromString(String auctionId) throws BadRequestException {
        if (!ObjectId.isValid(auctionId))
            throw new BadRequestException("Invalid auction ID: " + auctionId);
        return new ObjectId(auctionId);
    }

    private ObjectId questionIdFromString(String questionId) throws BadRequestException {
        if (!ObjectId.isValid(questionId))
            throw new BadRequestException("Invalid question ID: " + questionId);
        return new ObjectId(questionId);
    }

    @WithSpan
    private AuctionItem auctionDaoToItem(AuctionDao auctionDao) throws UserNotFoundException, BidNotFoundException {
        return this.auctionDaosToItems(List.of(auctionDao)).get(auctionDao.id);
    }

    @WithSpan
    private Map<ObjectId, AuctionItem> auctionDaosToItems(Collection<AuctionDao> auctionDaos)
            throws UserNotFoundException, BidNotFoundException {
        var userIds = auctionDaos.stream().map(a -> a.userId).collect(Collectors.toCollection(ArrayList::new));
        var auctionIds = auctionDaos.stream().map(a -> a.id).collect(Collectors.toCollection(ArrayList::new));
        var topBidDaos = this.repo.getAuctionTopBidMany(auctionIds);
        var topBidIds = topBidDaos.keySet().stream()
                .filter(aid -> topBidDaos.containsKey(aid))
                .collect(Collectors.toMap(aid -> aid, aid -> topBidDaos.get(aid).id));
        for (var bidDao : topBidDaos.values())
            userIds.add(bidDao.userId);
        var displayNames = this.repo.getUserDisplayNameMany(userIds);
        var bidItems = this.bidDaosToItems(topBidDaos.values());
        var auctions = new HashMap<ObjectId, AuctionItem>();
        for (var auctionDao : auctionDaos) {
            var auctionItem = new AuctionItem(
                    auctionDao.id.toHexString(),
                    auctionDao.title,
                    auctionDao.description,
                    displayNames.get(auctionDao.userId),
                    auctionDao.createTime,
                    auctionDao.closeTime,
                    Optional.ofNullable(auctionDao.imageId).map(Kube::stringToMediaId),
                    auctionDao.initialPrice,
                    auctionDaoStatusToAuctionStatus(auctionDao.status),
                    Optional.ofNullable(topBidIds.get(auctionDao.id)).map(bId -> bidItems.get(bId)));
            auctions.put(auctionDao.id, auctionItem);
        }
        return auctions;
    }

    @WithSpan
    private BidItem bidDaoToItem(BidDao bidDao) throws UserNotFoundException {
        return this.bidDaosToItems(List.of(bidDao)).get(bidDao.id);
    }

    @WithSpan
    private Map<ObjectId, BidItem> bidDaosToItems(Collection<BidDao> bidDaos) throws UserNotFoundException {
        var userIds = bidDaos.stream().map(b -> b.userId).collect(Collectors.toSet());
        var displayNames = this.repo.getUserDisplayNameMany(userIds);
        return this.bidDaosToItems(bidDaos, displayNames);
    }

    @WithSpan
    private Map<ObjectId, BidItem> bidDaosToItems(Collection<BidDao> bidDaos, Map<ObjectId, String> displayNames) {
        var bidItems = bidDaos.stream().collect(Collectors.toMap(b -> b.id, b -> new BidItem(
                b.id.toHexString(),
                b.auctionId.toHexString(),
                displayNames.get(b.userId),
                b.createTime,
                b.amount)));
        return bidItems;
    }

    @WithSpan
    private QuestionItem questionDaoToItem(QuestionDao questionDao) throws UserNotFoundException {
        return this.questionDaosToItems(List.of(questionDao)).get(0);
    }

    private List<QuestionItem> questionDaosToItems(Collection<QuestionDao> questionDaos) throws UserNotFoundException {
        var userIds = new HashSet<ObjectId>();
        for (var questionDao : questionDaos) {
            userIds.add(questionDao.userId);
            if (questionDao.reply != null)
                userIds.add(questionDao.reply.userId);
        }

        var userDisplayNames = this.repo.getUserDisplayNameMany(userIds);
        var questionItems = new ArrayList<QuestionItem>(questionDaos.size());
        for (var questionDao : questionDaos) {
            var replyItem = Optional.ofNullable(questionDao.reply)
                    .map(r -> new ReplyItem(
                            questionDao.id.toHexString(),
                            userDisplayNames.get(r.userId),
                            r.reply));
            var questionItem = new QuestionItem(
                    questionDao.id.toHexString(),
                    questionDao.auctionId.toHexString(),
                    userDisplayNames.get(questionDao.userId),
                    questionDao.question,
                    replyItem);
            questionItems.add(questionItem);
        }
        return questionItems;
    }

    @WithSpan
    private ReplyItem questionDaoToReplyItem(QuestionDao questionDao) throws UserNotFoundException {
        assert questionDao.reply != null;
        var questionItem = this.questionDaoToItem(questionDao);
        return questionItem.getReply().get();
    }

    private static AuctionStatus auctionDaoStatusToAuctionStatus(AuctionDao.Status status) {
        return switch (status) {
            case CLOSED -> AuctionStatus.CLOSED;
            case OPEN -> AuctionStatus.OPEN;
            default -> throw new IllegalStateException();
        };
    }
}
