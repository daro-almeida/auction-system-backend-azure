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

    void deleteAuction(AuctionDAO auctionDAO);

    void updateAuction(AuctionDAO oldValue, AuctionDAO newValue);

    void updateQuestion(QuestionDAO oldValue, QuestionDAO newValue); //happens on reply

    List<AuctionDAO> getAboutToCloseAuctions();

    void addAboutToCloseAuctions(List<AuctionDAO> auctions); // should only expire on 24h timer

    List<AuctionDAO> getRecentAuctions();

    void  addRecentAuction(AuctionDAO auctionDAO); // auctions created on the same day as the list call

    List<AuctionDAO> getPopularAuctions();

    void addPopularAuctions(AuctionDAO auctionDAO); // Top 5 auctions that have the most bids

    //Media

    void setMedia(String mediaId, byte[] contents);

    void deleteMedia(String mediaId);

    byte[] getMedia(String mediaId);
}