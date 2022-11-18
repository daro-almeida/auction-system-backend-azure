package scc.rest.dto;

import scc.item.QuestionItem;
import scc.item.ReplyItem;

public class QuestionDTO {
    public String id;
    public String auctionId;
    public String authorId;
    public String text;
    public String reply;

    public QuestionDTO() {
    }

    public QuestionDTO(String id, String auctionId, String authorId, String text, String reply) {
        this.id = id;
        this.auctionId = auctionId;
        this.authorId = authorId;
        this.text = text;
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

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public static QuestionDTO from(QuestionItem questionItem) {
        return new QuestionDTO(
                questionItem.getId(),
                questionItem.getAuctionId(),
                questionItem.getUserId(),
                questionItem.getQuestion(),
                questionItem.getReply().map(ReplyItem::getReply).orElse(null));
    }

    @Override
    public String toString() {
        return "QuestionDTO [id=" + id + ", auctionId=" + auctionId + ", authorId=" + authorId + ", text=" + text
                + ", reply=" + reply + "]";
    }
}
