package scc.data.client;

import java.util.Date;

import static scc.data.client.AuctionStatus.*;

/**
 * Represents an auction
 */

public class Auction {
    private final String id;
    private String title;
    private String description;
    private String pictureId;
    private final String userId;
    private Date endTime;
    private long minimumPrice;
    private String winnerBidId;
    private AuctionStatus status;

    public Auction(String title,
                   String description,
                   String pictureId,
                   String userId,
                   Date endTime,
                   long minimumPrice){
        this(generateAuctionId(), title, description, pictureId, userId, endTime, minimumPrice, null, OPEN);
    }

    public Auction(String id,
                   String title,
                   String description,
                   String pictureId,
                   String userId,
                   Date endTime,
                   long minimumPrice,
                   String winnerBidId,
                   AuctionStatus status){
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

    private static String generateAuctionId(){
        return "0:" + System.currentTimeMillis();
    }

    public String getId(){ return id;}
    public String getTitle() { return title;}
    public String getDescription() { return description;}
    public String getPictureId() { return pictureId;}
    public String getUserId() { return userId;}
    public Date getEndTime() { return endTime;}
    public long getMinimumPrice() { return minimumPrice;}
    public String getWinnerBidId() {return winnerBidId;}
    public AuctionStatus getStatus() { return status;}

    public void setTitle(String title) {this.title = title;}
    public void setDescription(String description) {this.description = description;}
    public void setPictureId(String pictureId) {this.pictureId = pictureId;}
    public void setEndTime(Date endTime) {this.endTime = endTime;}
    public void setMinimumPrice(long minimumPrice) {this.minimumPrice = minimumPrice;}
    public void setWinnerBidId(String winnerBidId) {this.winnerBidId = winnerBidId;}
    public void setStatus(AuctionStatus status) {this.status = status;}

    @Override
    public String toString(){
        return "Auction{" +
                "id='" + id + '\'' +
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
