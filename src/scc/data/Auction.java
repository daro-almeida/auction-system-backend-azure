package scc.data;

import java.util.Date;

import static scc.data.AuctionStatus.*;

/**
 * Represents an auction
 */

public class Auction {
    private final String id;
    private String title;
    private String description;
    private String pictureId;
    private String ownerId;
    private Date endTime;
    private long minimumPrice;
    private String winnerBidId;
    private AuctionStatus status;

    public Auction(String title,
                   String description,
                   String pictureId,
                   String ownerId,
                   Date endTime,
                   long minimumPrice,
                   String winnerBidId){
        this(generateAuctionId(), title, description, pictureId, ownerId, endTime, minimumPrice, winnerBidId);
    }

    public Auction(String id,
                   String title,
                   String description,
                   String pictureId,
                   String ownerId,
                   Date endTime,
                   long minimumPrice,
                   String winnerBidId){
        this.id = id;
        this.title = title;
        this.description = description;
        this.pictureId = pictureId;
        this.ownerId = ownerId;
        this.endTime = endTime;
        this.minimumPrice = minimumPrice;
        this.winnerBidId = winnerBidId;
        status = OPEN;
    }

    private static String generateAuctionId(){
        return "0:" + System.currentTimeMillis();
    }

    public String getId(){ return id;}

    public String getTitle() { return title;}

    public void setTitle(String title) {this.title = title;}

    public String getDescription() { return description;}

    public void setDescription(String description) {this.description = description;}

    public String getPictureId() { return pictureId;}

    public void setPictureId(String pictureId) {this.pictureId = pictureId;}

    public String getOwnerId() { return ownerId;}

    public String getEndTime() { return endTime.toString();}

    public void setEndTime(Date endTime) {this.endTime = endTime;}

    public long getMinimumPrice() { return minimumPrice;}

    public void setMinimumPrice(long minimumPrice) {this.minimumPrice = minimumPrice;}

    public AuctionStatus getStatus() { return status;}

    public void setStatus(AuctionStatus status) {this.status = status;}

    @Override
    public String toString(){
        return "Auction{" +
                "id='" + id + '\'' +
                "title='" + title + '\'' +
                "description='" + description + '\'' +
                "pictureId='" + pictureId + '\'' +
                "ownerId='" + ownerId + '\'' +
                "endTime='" + getEndTime() + '\'' +
                "minimumPrice='" + minimumPrice + '\'' +
                "winnerBidId='" + winnerBidId + '\'' + // return null if not closed?
                "status='" + status + '\'' + // Either the value or description
                '}';

    }
}
