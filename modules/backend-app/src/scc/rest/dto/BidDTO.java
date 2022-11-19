package scc.rest.dto;

import java.time.ZoneOffset;

import scc.item.BidItem;

public class BidDTO {
    private String id;
    private String auctionId;
    private String user;
    private String time;
    private double value;

    public BidDTO() {
    }

    public BidDTO(String id, String auctionId, String user, String time, double value) {
        this.id = id;
        this.auctionId = auctionId;
        this.user = user;
        this.time = time;
        this.value = value;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public static BidDTO from(BidItem item) {
        return new BidDTO(
                item.getId(),
                item.getAuctionId(),
                item.getBidderId(),
                item.getBidTime().atZone(ZoneOffset.UTC).toString(),
                item.getAmount());
    }

    @Override
    public String toString() {
        return "BidDTO [id=" + id + ", auctionId=" + auctionId + ", user=" + user + ", time=" + time + ", value="
                + value + "]";
    }
}
