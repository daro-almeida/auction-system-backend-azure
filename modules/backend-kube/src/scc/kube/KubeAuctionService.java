package scc.kube;

import java.util.List;

import scc.AuctionService;
import scc.Result;
import scc.ServiceError;
import scc.SessionToken;
import scc.UpdateAuctionOps;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;

public class KubeAuctionService implements AuctionService {

    @Override
    public Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<AuctionItem, ServiceError> getAuction(String auctionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void, ServiceError> updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<BidItem>, ServiceError> listAuctionBids(String auctionId) {
        // TODO Auto-generated method stub
        return null;
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
