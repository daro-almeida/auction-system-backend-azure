package scc.azure.repo;

import java.util.List;

import scc.Result;
import scc.ServiceError;
import scc.azure.dao.QuestionDAO;

public interface QuestionRepo {
    public Result<QuestionDAO, ServiceError> getQuestion(String id);

    public Result<QuestionDAO, ServiceError> insertQuestion(QuestionDAO question);

    public Result<QuestionDAO, ServiceError> insertReply(String id, QuestionDAO.Reply reply);

    public Result<List<QuestionDAO>, ServiceError> listAuctionQuestions(String auctionId);

    public Result<List<QuestionDAO>, ServiceError> queryQuestionsFromAuction(String auctionId, String query);
}
