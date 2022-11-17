package scc.azure.cache;

import java.util.List;

import scc.azure.dao.AuctionDAO;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;

public interface Cache {
    // User
    void addUserAuction(AuctionDAO auctionDAO);

    void removeUserAuction(AuctionDAO auctionDAO);

    List<AuctionDAO> getUserAuctions(String userId);

    void deleteUser(String userId);


    // Auction
    void addAuctionBid(BidDAO bidDAO);

    void removeAuctionBid(BidDAO bidDAO);

    List<BidDAO> getAuctionBids(String auctionId);

    void addAuctionQuestion(QuestionDAO questionDAO);

    void removeAuctionQuestion(QuestionDAO questionDAO);

    List<QuestionDAO> getAuctionQuestions(String auctionId);

    void deleteAuction(String auctionId);

    void updateAuction(AuctionDAO oldValue, AuctionDAO newValue);

    void updateQuestion(QuestionDAO oldValue, QuestionDAO newValue); //happens on reply


    //Media

    void setMedia(String mediaId, byte[] contents);

    void deleteMedia(String mediaId);

    byte[] getMedia(String mediaId);

    //TODO list Recent and popular auctions
}
