package scc.item;

public class ReplyItem {
    private String questionId;
    private String userId;
    private String reply;

    public ReplyItem(String questionId, String userId, String reply) {
        this.questionId = questionId;
        this.userId = userId;
        this.reply = reply;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    @Override
    public String toString() {
        return "ReplyItem [questionId=" + questionId + ", userId=" + userId + ", reply=" + reply + "]";
    }
}
