package scc.item;

import java.time.LocalDateTime;
import java.util.Optional;

import scc.AuctionStatus;
import scc.MediaId;

public class AuctionItem {
    private String id;
    private String title;
    private String description;
    private String ownerId;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private Optional<MediaId> imageId;
    private double startingPrice;
    private AuctionStatus status;
    private Optional<BidItem> topBid;

    public AuctionItem(String id,
            String title,
            String description,
            String ownerId,
            LocalDateTime openTime,
            LocalDateTime closeTime,
            Optional<MediaId> imageId,
            double startingPrice,
            AuctionStatus status,
            Optional<BidItem> topBid) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.imageId = imageId;
        this.startingPrice = startingPrice;
        this.status = status;
        this.topBid = topBid;
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

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setcloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public Optional<MediaId> getImageId() {
        return imageId;
    }

    public void setImageId(Optional<MediaId> imageId) {
        this.imageId = imageId;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public Optional<BidItem> getTopBid() {
        return topBid;
    }

    public void setTopBid(Optional<BidItem> topBid) {
        this.topBid = topBid;
    }

    @Override
    public String toString() {
        return "AuctionItem [id=" + id + ", title=" + title + ", description=" + description + ", ownerId=" + ownerId
                + ", imageId=" + imageId + ", startingPrice=" + startingPrice + ", status=" + status + ", topBid="
                + topBid + "]";
    }

}
