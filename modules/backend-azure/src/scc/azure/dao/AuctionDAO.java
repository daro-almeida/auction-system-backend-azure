package scc.azure.dao;

import java.time.LocalDateTime;

public class AuctionDAO {
    public static enum Status {
        OPEN,
        CLOSED,
        DELETED
    }

    private String _rid;
    private String _ts;
    private String id;
    private String title;
    private String description;
    private String pictureId;
    private String userId;
    private LocalDateTime endTime;
    private double startingPrice;
    private String winnerBidId;
    private Status status;

    public AuctionDAO(
            String title,
            String description,
            String pictureId,
            String userId,
            LocalDateTime endTime,
            double startingPrice) {
        this._rid = null;
        this._ts = null;
        this.id = null;
        this.title = title;
        this.description = description;
        this.pictureId = pictureId;
        this.userId = userId;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.winnerBidId = null;
        this.status = Status.OPEN;
    }

    public AuctionDAO(String id,
            String title,
            String description,
            String pictureId,
            String userId,
            LocalDateTime endTime,
            double startingPrice,
            String winnerBidId,
            Status status) {
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

    public AuctionDAO() {
    }

    public String get_rid() {
        return _rid;
    }

    public String get_ts() {
        return _ts;
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

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public String getWinnerBidId() {
        return winnerBidId;
    }

    public Status getStatus() {
        return status;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPictureId(String pictureId) {
        this.pictureId = pictureId;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStartingPrice(double minimumPrice) {
        this.startingPrice = minimumPrice;
    }

    public void setWinnerBidId(String winnerBidId) {
        this.winnerBidId = winnerBidId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AuctionDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", pictureId='" + pictureId + '\'' +
                ", userId='" + userId + '\'' +
                ", endTime='" + endTime + '\'' +
                ", minimumPrice='" + startingPrice + '\'' +
                ", winnerBidId='" + winnerBidId + '\'' + // return null if not closed?
                ", status='" + status + '\'' + // Either the value or description
                '}';

    }
}
