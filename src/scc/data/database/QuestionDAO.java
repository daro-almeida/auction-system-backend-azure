package scc.data.database;

import scc.data.client.Question;

public class QuestionDAO {
    private String _rid;
    private String _ts;
    private final String questionId;
    private final String auctionId;
    private final String userId;
    private final String description;

    public QuestionDAO(Question question){
        this(question.getQuestionId(), question.getAuctionId(), question.getUserId(), question.getDescription());
    }

    public QuestionDAO(String questionId, String auctionId, String userId, String description){
        super();
        this.questionId = questionId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.description = description;
    }

    public String getQuestionId() {return questionId;}
    public String getAuctionId() {return auctionId;}
    public String getUserId() {return userId;}
    public String getDescription() {return description;}

    public Question toQuestion() {
        return new Question(questionId, auctionId, userId, description);
    }

    @Override
    public String toString(){
        return "QuestionDAO:{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + questionId + '\'' +
                ", userId='" + auctionId + '\'' +
                ", userId='" + userId + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
