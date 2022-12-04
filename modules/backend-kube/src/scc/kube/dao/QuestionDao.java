package scc.kube.dao;

import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class QuestionDao {
    public static class Reply {
        public String reply;

        @BsonProperty(value = "create_time")
        public LocalDateTime createTime;

        @BsonProperty(value = "user_id")
        public ObjectId userId;
    }

    public ObjectId id;

    @BsonProperty(value = "auction_id")
    public ObjectId auctionId;

    @BsonProperty(value = "user_id")
    public ObjectId userId;

    public String question;

    @BsonProperty(value = "create_time")
    public LocalDateTime createTime;

    public Reply reply;

    public QuestionDao() {
    }

}
