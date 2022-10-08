package scc.data.database;

import scc.data.client.Reply;

public class ReplyDAO {
    private String _rid;
    private String _ts;
    private final String replyId;
    private final String questionId;
    private final String userId;
    private final String description;

    public ReplyDAO(Reply reply){
        this(reply.getReplyId(), reply.getQuestionId(), reply.getUserId(), reply.getDescription());
    }

    public ReplyDAO(String replyId, String questionId, String userId, String description){
        super();
        this.replyId = replyId;
        this.questionId = questionId;
        this.userId = userId;
        this.description = description;
    }

    public String getReplyId() {return replyId;}
    public String getQuestionId() {return questionId;}
    public String getUserId() {return userId;}
    public String getDescription() {return description;}

    @Override
    public String toString(){
        return "Reply:{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + replyId + '\'' +
                ", questionId='" + questionId + '\'' +
                ", userId='" + userId + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
