package scc.kube.dao;

import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuctionDao {
    public static enum Status {
        OPEN,
        CLOSED,
        DELETED
    }

    private ObjectId id;
    private String title;
    private String description;

    @JsonProperty("image_id")
    @BsonProperty(value = "image_id")
    private String imageId;

    @JsonProperty("user_id")
    @BsonProperty(value = "user_id")
    private ObjectId userId;

    @JsonProperty("create_time")
    @BsonProperty(value = "create_time")
    private LocalDateTime createTime;

    @JsonProperty("close_time")
    @BsonProperty(value = "close_time")
    private LocalDateTime closeTime;

    @JsonProperty("initial_price")
    @BsonProperty(value = "initial_price")
    private Double initialPrice;

    private Status status;

    // May not exist.
    // It is the winning bid if the status is CLOSED.
    @JsonProperty("highest_bid")
    @BsonProperty(value = "highest_bid")
    private ObjectId highestBid;

    public AuctionDao(ObjectId id, String title, String description, String imageId, ObjectId userId,
            LocalDateTime createTime, LocalDateTime closeTime, Double initialPrice, Status status,
            ObjectId highestBid) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageId = imageId;
        this.userId = userId;
        this.createTime = createTime;
        this.closeTime = closeTime;
        this.initialPrice = initialPrice;
        this.status = status;
        this.highestBid = highestBid;
    }

    public AuctionDao() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
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

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public Double getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(Double initialPrice) {
        this.initialPrice = initialPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ObjectId getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(ObjectId highestBid) {
        this.highestBid = highestBid;
    }

    @Override
    public String toString() {
        return "AuctionDao [id=" + id + ", title=" + title + ", description=" + description + ", imageId=" + imageId
                + ", userId=" + userId + ", createTime=" + createTime + ", closeTime=" + closeTime + ", initialPrice="
                + initialPrice + ", status=" + status + ", highestBid=" + highestBid + "]";
    }

}
