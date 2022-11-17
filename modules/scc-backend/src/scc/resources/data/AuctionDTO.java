package scc.resources.data;

import java.time.ZonedDateTime;

import scc.services.data.AuctionItem;

public class AuctionDTO {
    public String id;
    public String title;
    public String description;
    public String owner;
    public String imageId;
    public ZonedDateTime endTime;
    public double minimumPrice;
    public String status;
    public BidDTO bid;

    public AuctionDTO() {
    }

    public AuctionDTO(String id, String title, String description, String owner, String imageId, ZonedDateTime endTime,
            double minimumPrice, String status, BidDTO bid) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.owner = owner;
        this.imageId = imageId;
        this.endTime = endTime;
        this.minimumPrice = minimumPrice;
        this.status = status;
        this.bid = bid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public double getMinimumPrice() {
        return minimumPrice;
    }

    public void setMinimumPrice(double minimumPrice) {
        this.minimumPrice = minimumPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BidDTO getBid() {
        return bid;
    }

    public void setBid(BidDTO bid) {
        this.bid = bid;
    }

    public static AuctionDTO from(AuctionItem item) {
        return new AuctionDTO(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getUserId(),
                item.getPictureId(),
                item.getEndTime(),
                item.getStartingPrice(),
                item.getStatus().toString(),
                item.getWinnerBidId().map(BidDTO::from).orElse(null));
    }

    @Override
    public String toString() {
        return "AuctionDTO [id=" + id + ", title=" + title + ", description=" + description + ", owner=" + owner
                + ", imageId=" + imageId + ", endTime=" + endTime + ", minimumPrice=" + minimumPrice + ", status="
                + status + ", bid=" + bid + "]";
    }

}
