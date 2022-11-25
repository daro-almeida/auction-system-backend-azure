package scc.kube.dao;

import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BidDao {
    // Mandatory fields
    private ObjectId id;

    @JsonProperty("auction_id")
    @BsonProperty(value = "auction_id")
    private ObjectId auctionId;

    @JsonProperty("user_id")
    @BsonProperty(value = "user_id")
    private String userId;

    private Double value;
    private LocalDateTime time;

    public BidDao(ObjectId id, ObjectId auctionId, String userId, Double value, LocalDateTime time) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.value = value;
        this.time = time;
    }

    public BidDao() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(ObjectId auctionId) {
        this.auctionId = auctionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "BidDao [id=" + id + ", auctionId=" + auctionId + ", userId=" + userId + ", value=" + value + ", time="
                + time + "]";
    }

}
