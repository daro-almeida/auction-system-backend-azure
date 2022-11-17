package scc.services.data;

import scc.azure.dao.AuctionDAO;

import java.time.ZonedDateTime;
import java.util.Optional;

public class AuctionItem {

    private final String id;
    private final String title;
    private final String description;
    private final String pictureId;
    private final String userId;
    private final ZonedDateTime endTime;
    private final double startingPrice;
    private final Optional<BidItem> winnerBidId;
    private final AuctionDAO.Status status;

    public AuctionItem(String id, String title, String description, String pictureId, String userId,
            ZonedDateTime endTime,
            double startingPrice, Optional<BidItem> winnerBidId, AuctionDAO.Status status) {
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

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public Optional<BidItem> getWinnerBidId() {
        return winnerBidId;
    }

    public AuctionDAO.Status getStatus() {
        return status;
    }
}
