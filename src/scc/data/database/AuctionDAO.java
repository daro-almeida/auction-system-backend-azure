package scc.data.database;

import scc.data.client.Auction;
import scc.data.client.AuctionStatus;

import java.util.Date;

public class AuctionDAO {
    private String _rid;
    private String _ts;
    private final String id;
    private String title;
    private String description;
    private String pictureId;
    private String userId;
    private Date endTime;
    private long minimumPrice;
    private String winnerBidId;
    private AuctionStatus status;

    public AuctionDAO(Auction auction){
        this(auction.getId(),
                auction.getTitle(),
                auction.getDescription(),
                auction.getPictureId(),
                auction.getUserId(),
                auction.getEndTime(),
                auction.getMinimumPrice(),
                auction.getWinnerBidId(),
                auction.getStatus());
    }

    public AuctionDAO(String id,
                      String title,
                      String description,
                      String pictureId,
                      String userId,
                      Date endTime,
                      long minimumPrice,
                      String winnerBidId,
                      AuctionStatus status){
        super();
        this.id = id;
        this.title = title;
        this.description = description;
        this.pictureId = pictureId;
        this.userId = userId;
        this.endTime = endTime;
        this.minimumPrice = minimumPrice;
        this.winnerBidId = winnerBidId;
        this.status = status;
    }

    public String get_rid() {
        return _rid;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
    }

    public String getId(){ return id;}

    public String getTitle() { return title;}

    public void setTitle(String title) {this.title = title;}

    public String getDescription() { return description;}

    public void setDescription(String description) {this.description = description;}

    public String getPictureId() { return pictureId;}

    public void setPictureId(String pictureId) {this.pictureId = pictureId;}

    public String getUserId() { return userId;}

    public Date getEndTime() { return endTime;}

    public void setEndTime(Date endTime) {this.endTime = endTime;}

    public long getMinimumPrice() { return minimumPrice;}

    public void setMinimumPrice(long minimumPrice) {this.minimumPrice = minimumPrice;}

    public String getWinnerBidId() {return winnerBidId;}

    public void setWinnerBidId(String winnerBidId) {this.winnerBidId = winnerBidId;}

    public AuctionStatus getStatus() { return status;}

    public void setStatus(AuctionStatus status) {this.status = status;}

    public Auction toAuction() {return new Auction(id, title, description, pictureId, userId, endTime, minimumPrice, winnerBidId, status);}

    @Override
    public String toString(){
        return "AuctionDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", pictureId='" + pictureId + '\'' +
                ", userId='" + userId + '\'' +
                ", endTime='" + endTime + '\'' +
                ", minimumPrice='" + minimumPrice + '\'' +
                ", winnerBidId='" + winnerBidId + '\'' + // return null if not closed?
                ", status='" + status + '\'' + // Either the value or description
                '}';

    }
}
