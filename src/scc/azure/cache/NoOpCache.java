package scc.azure.cache;

import java.util.List;

import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
import scc.azure.dao.UserDAO;

public class NoOpCache implements Cache {

    @Override
    public void addUserAuction(AuctionDAO auctionDAO) {

    }

    @Override
    public void removeUserAuction(AuctionDAO auctionDAO) {

    }

    @Override
    public List<AuctionDAO> getUserAuctions(String userId) {
        return null;
    }

    @Override
    public void deleteUser(String userId) {

    }

    @Override
    public void addAuctionBid(BidDAO bidDAO) {

    }

    @Override
    public void removeAuctionBid(BidDAO bidDAO) {

    }

    @Override
    public List<BidDAO> getAuctionBids(String auctionId) {
        return null;
    }

    @Override
    public void addAuctionQuestion(QuestionDAO questionDAO) {

    }

    @Override
    public void removeAuctionQuestion(QuestionDAO questionDAO) {

    }

    @Override
    public List<QuestionDAO> getAuctionQuestions(String auctionId) {
        return null;
    }

    @Override
    public void deleteAuction(String auctionId) {

    }

    @Override
    public void updateAuction(AuctionDAO oldValue, AuctionDAO newValue) {

    }

    @Override
    public void updateQuestion(QuestionDAO oldValue, QuestionDAO newValue) {

    }

    @Override
    public List<AuctionDAO> getAboutToCloseAuctions() {
        return null;
    }

    @Override
    public void addAboutToCloseAuctions(List<AuctionDAO> auctions) {

    }

    @Override
    public List<AuctionDAO> getRecentAuctions() {
        return null;
    }

    @Override
    public void addRecentAuction(AuctionDAO auctionDAO) {

    }

    @Override
    public List<AuctionDAO> getPopularAuctions() {
        return null;
    }

    @Override
    public void addPopularAuctions(AuctionDAO auctionDAO) {

    }

    @Override
    public void setMedia(String mediaId, byte[] contents) {

    }

    @Override
    public void deleteMedia(String mediaId) {

    }

    @Override
    public byte[] getMedia(String mediaId) {
        return null;
    }
}
