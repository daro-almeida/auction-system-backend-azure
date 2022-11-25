package scc.kube.dao;

import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class BidDao {
    public ObjectId id;

    @BsonProperty(value = "auction_id")
    public ObjectId userId;

    @BsonProperty(value = "user_id_display")
    public String userIdDisplay;
    public double amount;

    @BsonProperty(value = "create_time")
    public LocalDateTime createTime;

    public BidDao() {
    }
}
