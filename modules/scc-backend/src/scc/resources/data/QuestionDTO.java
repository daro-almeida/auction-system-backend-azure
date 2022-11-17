package scc.resources.data;

import java.util.Optional;

public class QuestionDTO {
    public String authorId;
    public String content;
    public Optional<ReplyDTO> reply;

    public QuestionDTO() {
    }

    public QuestionDTO(String authorId, String content) {
        this.authorId = authorId;
        this.content = content;
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

    @Override
    public String toString() {
        return "QuestionDTO [authorId=" + authorId + ", content=" + content + "]";
    }

    public static QuestionDTO from(scc.services.data.QuestionItem questionItem) {
        return new QuestionDTO(questionItem.getAuthorId(), questionItem.getQuestion());
    }
}
