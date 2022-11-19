package scc.azure.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDAO {
@JsonIgnoreProperties(ignoreUnknown = true)
public static class Reply {
        private String userId;
        private String reply;

        public Reply(String userId, String reply) {
            this.userId = userId;
            this.reply = reply;
        }

        public Reply() {
        }

        public String getUserId() {
            return userId;
        }

        public String getReply() {
            return reply;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }

        @Override
        public String toString() {
            return "Reply [userId=" + userId + ", reply=" + reply + "]";
        }
    }

    private String _rid;
    private String _ts;
    private String id;
    private String auctionId;
    private String userId;
    private String question;
    private Reply reply;

    public QuestionDAO(String auctionId, String userId, String question) {
        this._rid = null;
        this._ts = null;
        this.id = null;
        this.auctionId = auctionId;
        this.userId = userId;
        this.question = question;
        this.reply = null;
    }

    public QuestionDAO(String id, String auctionId, String userId, String question, Reply reply) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.question = question;
        this.reply = reply;
    }

    public QuestionDAO() {
    }

    public String get_rid() {
        return _rid;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
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

    public Reply getReply() {
        return reply;
    }

    public void setReply(Reply reply) {
        this.reply = reply;
    }

    @Override
    public String toString() {
        return "QuestionDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", auctionId=" + auctionId + ", userId="
                + userId + ", question=" + question + ", reply=" + reply + "]";
    }

}
