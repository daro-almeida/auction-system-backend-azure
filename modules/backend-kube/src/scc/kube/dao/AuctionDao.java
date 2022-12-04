package scc.kube.dao;

import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class AuctionDao {
    public static enum Status {
        OPEN, CLOSED
    }

    public ObjectId id;
    public String title;
    public String description;

    @BsonProperty(value = "image_id")
    public String imageId;

    @BsonProperty(value = "user_id")
    public ObjectId userId;

    @BsonProperty(value = "create_time")
    public LocalDateTime createTime;

    @BsonProperty(value = "close_time")
    public LocalDateTime closeTime;

    @BsonProperty(value = "initial_price")
    public double initialPrice;

    public Status status;

    public AuctionDao() {
    }
}