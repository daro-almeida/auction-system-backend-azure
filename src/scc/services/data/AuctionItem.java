package scc.services.data;

import scc.azure.dao.AuctionDAO;
import scc.services.AuctionService;
import scc.services.ServiceError;
import scc.utils.Result;

import java.util.Date;

public class AuctionItem {

    private final String id;
    private final String title;
    private final String description;
    private final String pictureId;
    private final String userId;
    private final Date endTime;
    private final long startingPrice;
    private final String winnerBidId;
    private final AuctionDAO.Status status;

    public AuctionItem(String id, String title, String description, String pictureId, String userId, Date endTime,
                       long startingPrice, String winnerBidId, AuctionDAO.Status status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.pictureId = pictureId;
        this.userId = userId;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.winnerBidId = winnerBidId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPictureId() {
        return pictureId;
    }

    public String getUserId() {
        return userId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public long getStartingPrice() {
        return startingPrice;
    }

    public String getWinnerBidId() {
        return winnerBidId;
    }

    public AuctionDAO.Status getStatus() {
        return status;
    }

    public static AuctionItem fromAuctionDAO(AuctionDAO auctionDAO) {
        return new AuctionItem(auctionDAO.getId(), auctionDAO.getTitle(), auctionDAO.getDescription(),
                auctionDAO.getPictureId(), auctionDAO.getUserId(), auctionDAO.getEndTime(),
                auctionDAO.getStartingPrice(), auctionDAO.getWinnerBidId(), auctionDAO.getStatus());
    }
}
