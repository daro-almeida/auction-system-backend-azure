package scc.azure.cache;

import java.util.List;

import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;

public class NoOpCache implements Cache {

    @Override
    public void setUser(UserDAO user) {
    }

    @Override
    public void unsetUser(String userId) {
    }

    @Override
    public UserDAO getUser(String userId) {
        return null;
    }

    @Override
    public void setAuction(AuctionDAO auction) {
    }

    @Override
    public void unsetAuction(String auctionId) {
    }

    @Override
    public AuctionDAO getAuction(String auctionId) {
        return null;
    }

    @Override
    public void setBid(BidDAO bid) {
    }

    @Override
    public void unsetBid(String bidId) {
    }

    @Override
    public BidDAO getBid(String bidId) {
        return null;
    }

    @Override
    public void setQuestion(QuestionDAO question) {
    }

    @Override
    public void unsetQuestion(String questionId) {
    }

    @Override
    public QuestionDAO getQuestion(String questionId) {
        return null;
    }

    @Override
    public void addAuctionQuestion(String auctionId, String questionId) {
    }

    @Override
    public void removeAuctionQuestion(String auctionId, String questionId) {
    }

    @Override
    public List<String> getAuctionQuestions(String auctionId) {
        return null;
    }

}
