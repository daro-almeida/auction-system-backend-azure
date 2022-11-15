package scc.azure.cache;

import java.util.List;

import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;

public interface Cache {
    // User
    void setUser(UserDAO user);

    void unsetUser(String userId);

    UserDAO getUser(String userId);

    // Auction
    void setAuction(AuctionDAO auction);

    void unsetAuction(String auctionId);

    AuctionDAO getAuction(String auctionId);

    // Bid
    void setBid(BidDAO bid);

    void unsetBid(String bidId);

    BidDAO getBid(String bidId);

    // Question
    void setQuestion(QuestionDAO question);

    void unsetQuestion(String questionId);

    QuestionDAO getQuestion(String questionId);

    // Auction question list
    void addAuctionQuestion(String auctionId, String questionId);

    void removeAuctionQuestion(String auctionId, String questionId);

    List<String> getAuctionQuestions(String auctionId);
}
