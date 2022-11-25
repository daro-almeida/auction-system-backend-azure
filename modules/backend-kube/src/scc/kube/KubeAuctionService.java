package scc.kube;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;

import scc.AuctionService;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateAuctionOps;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;

public class KubeAuctionService implements AuctionService {

    private KubeData data;
    private Mongo mongo;
    private Auth auth;

    @Override
    public Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params) {
        var authResult = this.auth.match(token, params.owner());
        if (authResult.isError())
            return Result.err(authResult);

        if (params.title().isBlank() || params.description().isBlank() || params.startingPrice() <= 0)
            return Result.err(ServiceError.BAD_REQUEST);

        var auctionDao = new AuctionDao(
                null,
                params.title(),
                params.description(),
                null, // TODO: fix this
                params.owner(),
                LocalDateTime.now(ZoneOffset.UTC),
                params.endTime(),
                params.startingPrice(),
                AuctionDao.Status.OPEN,
                null);

        auctionDao = this.mongo.createAuction(auctionDao);
        var auctionItemResult = this.data.auctionDaoToItem(auctionDao);
        if (auctionItemResult.isError())
            return Result.err(auctionItemResult);

        var auctionItem = auctionItemResult.value();
        this.data.setAuction(auctionDao);

        return Result.ok(auctionItem);
    }

    @Override
    public Result<AuctionItem, ServiceError> getAuction(String auctionIdStr) {
        if (!ObjectId.isValid(auctionIdStr))
            return Result.err(ServiceError.BAD_REQUEST);

        var auctionId = new ObjectId(auctionIdStr);
        var auctionResult = this.data.getAuction(auctionId);
        if (auctionResult.isError())
            return Result.err(auctionResult);
        var auctioDao = auctionResult.value();

        var auctionItemResult = this.data.auctionDaoToItem(auctioDao);
        if (auctionItemResult.isError())
            return Result.err(auctionItemResult);

        return Result.ok(auctionItemResult.value());
    }

    @Override
    public Result<Void, ServiceError> updateAuction(SessionToken token, String auctionIdStr, UpdateAuctionOps ops) {
        if (!ObjectId.isValid(auctionIdStr))
            return Result.err(ServiceError.BAD_REQUEST);

        var auctionId = new ObjectId(auctionIdStr);
        var authResult = this.auth.validate(token);
        if (authResult.isError())
            return Result.err(authResult);
        var userId = authResult.value();

        var auctionGetResult = this.data.getAuction(auctionId);
        if (auctionGetResult.isError())
            return Result.err(auctionGetResult);

        var auctionDao = auctionGetResult.value();
        if (!auctionDao.getUserId().equals(userId))
            return Result.err(ServiceError.UNAUTHORIZED);

        if (ops.shouldUpdateTitle())
            auctionDao.setTitle(ops.getTitle());
        if (ops.shouldUpdateDescription())
            auctionDao.setDescription(ops.getDescription());
        // if (ops.shouldUpdateImage()) // TODO: fix this
        // auctionDao.setPictureId(Azure.mediaIdToString(ops.getImage()));

        auctionDao = this.mongo.updateAuction(auctionId, auctionDao);
        this.data.setAuction(auctionDao);

        return Result.ok();
    }

    @Override
    public Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params) {
        if (!ObjectId.isValid(params.auctionId()))
            return Result.err(ServiceError.BAD_REQUEST);

        var authResult = this.auth.match(token, params.userId());
        if (authResult.isError())
            return Result.err(authResult);

        var auctionId = new ObjectId(params.auctionId());
        var bidDao = new BidDao(
                null,
                auctionId,
                params.userId(),
                params.price(),
                LocalDateTime.now(ZoneOffset.UTC));
        bidDao = this.mongo.createBid(bidDao);

        var auctionDaoResult = this.data.getAuction(auctionId);
        if (auctionDaoResult.isError())
            return Result.err(auctionDaoResult);
        var auctionDao = auctionDaoResult.value();

        if (!auctionDao.getStatus().equals(AuctionDao.Status.OPEN))
            return Result.err(ServiceError.AUCTION_NOT_OPEN);

        if (auctionDao.getHighestBid() != null) {
            var highestBidDaoResult = this.data.getBid(auctionDao.getHighestBid());
            if (highestBidDaoResult.isError())
                return Result.err(highestBidDaoResult.error());

            var highestBidDao = highestBidDaoResult.value();
            if (bidDao.getValue() <= highestBidDao.getValue())
                return Result.err(ServiceError.BID_CONFLICT);
        }

        var updateResult = this.mongo.updateHighestBid(auctionDao, bidDao);
        if (updateResult.isError())
            return Result.err(updateResult.error());
        this.data.invalidateAuction(auctionDao.getId());

        var userDaoResult = this.data.getUser(params.userId());
        if (userDaoResult.isError())
            return Result.err(userDaoResult);
        var userDao = userDaoResult.value();
        var bidItem = this.data.bidDaoToItem(bidDao, userDao);

        // Update user's following auctions
        // TODO: Update user's following auctions

        return Result.ok(bidItem);
    }

    @Override
    public Result<List<BidItem>, ServiceError> listAuctionBids(String auctionIdStr) {
        if (!ObjectId.isValid(auctionIdStr))
            return Result.err(ServiceError.BAD_REQUEST);

        var auctionId = new ObjectId(auctionIdStr);
        var listResult = this.data.listAuctionBidItems(auctionId, 0, Integer.MAX_VALUE);

        return listResult;
    }

    @Override
    public Result<QuestionItem, ServiceError> createQuestion(SessionToken token, CreateQuestionParams params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<ReplyItem, ServiceError> createReply(SessionToken token, CreateReplyParams params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> listAuctionQuestions(String auctionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId, boolean open) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsFollowedByUser(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listRecentAuctions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listPopularAuctions() {
        // TODO Auto-generated method stub
        return null;
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
