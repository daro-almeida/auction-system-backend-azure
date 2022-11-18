package scc.rest.dto;

import scc.item.ReplyItem;

public class ReplyDTO {
    public String authorId;
    public String content;

    public ReplyDTO() {
    }

    public ReplyDTO(String authorId, String content) {
        this.authorId = authorId;
        this.content = content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static ReplyDTO from(ReplyItem replyItem) {
        return new ReplyDTO(replyItem.getUserId(), replyItem.getReply());
    }

    @Override
    public String toString() {
        return "ReplyDTO [authorId=" + authorId + ", content=" + content + "]";
    }
}
