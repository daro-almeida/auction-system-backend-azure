package scc.item;

import java.time.LocalDateTime;

public class BidItem {
    private String id;
    private String auctionId;
    private String bidderId;
    private LocalDateTime bidTime;
    private double amount;

    public BidItem(String id, String auctionId, String bidderId, LocalDateTime bidTime, double amount) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidTime = bidTime;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

}
