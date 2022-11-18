package scc.rest.dto;

import java.util.Optional;

import scc.item.QuestionItem;

public class QuestionDTO {
    public String authorId;
    public String content;
    public Optional<ReplyDTO> reply;

    public QuestionDTO() {
    }

    public QuestionDTO(String authorId, String content, Optional<ReplyDTO> reply) {
        this.authorId = authorId;
        this.content = content;
        this.reply = reply;
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

    public Optional<ReplyDTO> getReply() {
        return reply;
    }

    public void setReply(Optional<ReplyDTO> reply) {
        this.reply = reply;
    }

    public static QuestionDTO from(QuestionItem questionItem) {
        return new QuestionDTO(
                questionItem.getUserId(),
                questionItem.getQuestion(),
                questionItem.getReply().map(ReplyDTO::from));
    }

    @Override
    public String toString() {
        return "QuestionDTO [authorId=" + authorId + ", content=" + content + "]";
    }
}
