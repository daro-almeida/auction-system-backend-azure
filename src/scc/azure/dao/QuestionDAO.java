package scc.azure.dao;

public class QuestionDAO {
    public static class Reply {
        private final String userId;
        private final String reply;

        public Reply(String userId, String reply) {
            this.userId = userId;
            this.reply = reply;
        }

        public String getUserId() {
            return userId;
        }

        public String getReply() {
            return reply;
        }

        @Override
        public String toString() {
            return "Reply [userId=" + userId + ", reply=" + reply + "]";
        }
    }

    private String _rid;
    private String _ts;
    private String questionId;
    private String auctionId;
    private String userId;
    private String question;
    private Reply reply;

    public QuestionDAO(String auctionId, String userId, String question) {
        this._rid = null;
        this._ts = null;
        this.questionId = null;
        this.auctionId = auctionId;
        this.userId = userId;
        this.question = question;
        this.reply = null;
    }

    public QuestionDAO(String questionId, String auctionId, String userId, String question, Reply reply) {
        this.questionId = questionId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.question = question;
        this.reply = reply;
    }

    public QuestionDAO() { }

    public String getQuestionId() {
        return questionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getQuestion() {
        return question;
    }

    public Reply getReply() {
        return reply;
    }

    @Override
    public String toString() {
        return "QuestionDAO:{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + questionId + '\'' +
                ", userId='" + auctionId + '\'' +
                ", userId='" + userId + '\'' +
                ", description='" + question + '\'' +
                ", reply='" + reply + '\'' +
                '}';
    }
}
