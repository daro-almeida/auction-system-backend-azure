package scc.utils;

import java.util.List;

import scc.AuctionService;
import scc.PagingWindow;
import scc.SessionToken;
import scc.UpdateAuctionOps;
import scc.exception.ServiceException;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;

public class AuctionServiceWithResources implements AuctionService {

    private final AuctionService auctionService;
    private final List<AutoCloseable> resources;

    public AuctionServiceWithResources(AuctionService auctionService, List<AutoCloseable> resources) {
        this.auctionService = auctionService;
        this.resources = resources;
    }

    @Override
    public void close() throws Exception {
        for (var resource : this.resources)
            resource.close();
    }

    @Override
    public AuctionItem createAuction(SessionToken token, CreateAuctionParams params) throws ServiceException {
        return this.auctionService.createAuction(token, params);
    }

    @Override
    public AuctionItem getAuction(String auctionId) throws ServiceException {
        return this.auctionService.getAuction(auctionId);
    }

    @Override
    public void updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops) throws ServiceException {
        this.auctionService.updateAuction(token, auctionId, ops);
    }

    @Override
    public BidItem createBid(SessionToken token, CreateBidParams params) throws ServiceException {
        return this.auctionService.createBid(token, params);
    }

    @Override
    public List<BidItem> listAuctionBids(String auctionId, PagingWindow window) throws ServiceException {
        return this.auctionService.listAuctionBids(auctionId, window);
    }

    @Override
    public QuestionItem createQuestion(SessionToken token, CreateQuestionParams params) throws ServiceException {
        return this.auctionService.createQuestion(token, params);
    }

    @Override
    public ReplyItem createReply(SessionToken token, CreateReplyParams params) throws ServiceException {
        return this.auctionService.createReply(token, params);
    }

    @Override
    public List<QuestionItem> listAuctionQuestions(String auctionId, PagingWindow window) throws ServiceException {
        return this.auctionService.listAuctionQuestions(auctionId, window);
    }

    @Override
    public List<AuctionItem> listUserAuctions(String username, boolean open) throws ServiceException {
        return this.auctionService.listUserAuctions(username, open);
    }

    @Override
    public List<AuctionItem> listAuctionsFollowedByUser(String username) throws ServiceException {
        return this.auctionService.listAuctionsFollowedByUser(username);
    }

    @Override
    public List<AuctionItem> listAuctionsAboutToClose() throws ServiceException {
        return this.auctionService.listAuctionsAboutToClose();
    }

    @Override
    public List<AuctionItem> listRecentAuctions() throws ServiceException {
        return this.auctionService.listRecentAuctions();
    }

    @Override
    public List<AuctionItem> listPopularAuctions() throws ServiceException {
        return this.auctionService.listPopularAuctions();
    }

}
