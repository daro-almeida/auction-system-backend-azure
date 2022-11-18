package scc.item;

import java.util.Optional;

public class QuestionItem {
    private String id;
    private String auctionId;
    private String userId;
    private String question;
    private Optional<ReplyItem> reply;

    public QuestionItem(String id, String auctionId, String userId, String question, Optional<ReplyItem> reply) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.question = question;
        this.reply = reply;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Optional<ReplyItem> getReply() {
        return reply;
    }

    public void setReply(Optional<ReplyItem> reply) {
        this.reply = reply;
    }

}
