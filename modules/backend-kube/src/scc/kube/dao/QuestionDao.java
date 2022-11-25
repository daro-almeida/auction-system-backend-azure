package scc.kube.dao;

import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestionDao {

    private ObjectId id;

    @JsonProperty("auction_id")
    @BsonProperty(value = "auction_id")
    private ObjectId auctionId;

    @JsonProperty("user_id")
    @BsonProperty(value = "user_id")
    private ObjectId userId;

    private String question;
    private LocalDateTime time;

    // Reply may not exist
    private String reply;

    @JsonProperty("reply_time")
    @BsonProperty(value = "reply_time")
    private LocalDateTime replyTime;

    public QuestionDao(ObjectId id, ObjectId auctionId, ObjectId userId, String question, LocalDateTime time,
            String reply, LocalDateTime replyTime) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.question = question;
        this.time = time;
        this.reply = reply;
        this.replyTime = replyTime;
    }

    public QuestionDao() {
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

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public LocalDateTime getReplyTime() {
        return replyTime;
    }

    public void setReplyTime(LocalDateTime replyTime) {
        this.replyTime = replyTime;
    }

    @Override
    public String toString() {
        return "QuestionDao [id=" + id + ", auctionId=" + auctionId + ", userId=" + userId + ", question=" + question
                + ", time=" + time + ", reply=" + reply + ", replyTime=" + replyTime + "]";
    }

}
