package scc.data.client;

public class Reply {
    private String replyId;
    private String questionId;
    private String userId;
    private String description;

    public Reply(String questionId, String userId, String description){
        this(generateReplyId(), questionId, userId, description);
    }

    public Reply(String replyId, String questionId, String userId, String description){
        this.replyId = replyId;
        this.questionId = questionId;
        this.userId = userId;
        this.description = description;
    }
    private static String generateReplyId(){
        return "0:" + System.currentTimeMillis();
    }

    public String getReplyId() {return replyId;}
    public String getQuestionId() {return questionId;}
    public String getUserId() {return userId;}
    public String getDescription() {return description;}

    @Override
    public String toString(){
        return "Reply:{" +
                "id='" + replyId + '\'' +
                ", questionId='" + questionId + '\'' +
                ", userId='" + userId + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
