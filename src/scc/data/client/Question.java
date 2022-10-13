package scc.data.client;

/**
 * Represents a question asked in an auction
 */
public class Question {
    private final String questionId;
    private final String auctionId;
    private final String userId;
    private final String description;
    private String replyDescription;

    public Question(String auctionId, String userId, String description){
        this(generateQuestionId(), auctionId, userId, description);
    }

    public Question(String questionId, String auctionId, String userId, String description){
        this.questionId = questionId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.description = description;
        replyDescription = null;
    }

    private static String generateQuestionId(){
        return "0:" + System.currentTimeMillis();
    }

    public String getQuestionId() {return questionId;}
    public String getAuctionId() {return auctionId;}
    public String getUserId() {return userId;}
    public String getDescription() {return description;}
    public String getReplyDescription() {return replyDescription;}

    public void setReplyDescription(String replyDescription) {this.replyDescription = replyDescription;}

    @Override
    public String toString(){
        return "Question:{" +
                "id='" + questionId + '\'' +
                ", auctionId='" + auctionId + '\'' +
                ", userId='" + userId + '\'' +
                ", description='" + description + '\'' +
                ", replyDescription='" + replyDescription + '\'' +
                '}';
    }
}
