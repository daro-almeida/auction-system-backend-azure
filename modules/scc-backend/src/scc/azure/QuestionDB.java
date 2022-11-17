package scc.azure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.QuestionDAO;
import scc.services.ServiceError;
import scc.utils.Result;

class QuestionDB {
    private final CosmosContainer container;

    public QuestionDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.questionContainer);
    }

    /**
     * Returns the question present in the database with given identifier
     * 
     * @param questionId identifier of the question
     * @return Object that represents the question
     */
    public Optional<QuestionDAO> getQuestion(String questionId) {
        var options = this.createQueryOptions(questionId);
        return this.container
                .queryItems(
                        "SELECT * FROM questions WHERE questions.id=\"" + questionId + "\"",
                        options,
                        QuestionDAO.class)
                .stream()
                .findFirst();
    }

    /**
     * Checks if the question with given identifier exists in the database
     * 
     * @param questionId identifier of the question
     * @return true if it exists, false otherwise
     */
    public boolean questionExists(String questionId) {
        return this.getQuestion(questionId).isPresent();
    }

    /**
     * Inserts an entry in the database that represents a question in an auction
     * 
     * @param question Object that represents a question
     * @return 200 with question's identifier
     */
    public Result<QuestionDAO, ServiceError> createQuestion(QuestionDAO question) {
        if (question.getId() == null)
            question.setId(UUID.randomUUID().toString());
        var response = this.container.createItem(question);
        return Result.ok(response.getItem());
    }

    /**
     * Updates the question with a reply to it in the database
     * 
     * @param questionId identifier of the question
     * @param reply      Object that represents a reply to the question
     * @return 200 with reply's identifier
     */
    public Result<QuestionDAO, ServiceError> createReply(String questionId, QuestionDAO.Reply reply) {
        var question = this.getQuestion(questionId);
        if (question.isEmpty())
            return Result.err(ServiceError.QUESTION_NOT_FOUND);
        if (question.get().getReply() != null)
            return Result.err(ServiceError.QUESTION_ALREADY_REPLIED);

        var partitionKey = this.createPartitionKey(questionId);
        var patch = CosmosPatchOperations.create();
        patch.add("/reply", reply);
        var response = this.container.patchItem(questionId, partitionKey, patch, QuestionDAO.class);

        return Result.ok(response.getItem());
    }

    /**
     * Gathers all questions that are saved in an auction with given identifier
     * 
     * @param auctionId identifier of the auction
     * @return List of questions present in the auction
     */
    public List<QuestionDAO> listQuestions(String auctionId) {
        var options = this.createQueryOptions(auctionId);
        return this.container
                .queryItems(
                        "SELECT * FROM questions WHERE questions.auctionId=\"" + auctionId + "\"",
                        options,
                        QuestionDAO.class)
                .stream().collect(Collectors.toList());
    }

    /**
     * Creates a partition key with given identifier of a question
     * 
     * @param questionId identifier of the question
     * @return PartitionKey with identifier of the question
     */
    private PartitionKey createPartitionKey(String questionId) {
        return new PartitionKey(questionId);
    }

    /**
     * Creates a QueryOptions object with a partition key of the identifier of the
     * question
     * 
     * @param questionId identifier of the question
     * @return QueryOptions object with mentioned partition key
     */
    private CosmosQueryRequestOptions createQueryOptions(String questionId) {
        var options = new CosmosQueryRequestOptions();
        options.setPartitionKey(this.createPartitionKey(questionId));
        return options;
    }

    /**
     * Delete all questions made by the user with given identifier
     * @param userId identifier of the user
     * @return 204 if successful
     */
    public Result<Void, ServiceError> deleteQuestionFromUser(String userId) {
        for (QuestionDAO question : userQuestions(userId)) {
            var result = deleteQuestion(question.getId());
            if (!result.isOk())
                System.out.printf("deleteUserQuestions: question %s not found\n", question.getId());
        }
        return Result.ok();
    }

    /**
     * Delete a question with given identifier from the database
     * @param questionId identifier of the question
     * @return 204 if successful,
     * 404 if it doesn't exist
     */
    private Result<String, ServiceError> deleteQuestion(String questionId) {
        var options = new CosmosItemRequestOptions();
        var partitionKey = this.createPartitionKey(questionId);
        try {
            this.container.deleteItem(questionId, partitionKey, options);
            return Result.ok(questionId);
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(ServiceError.QUESTION_NOT_FOUND);
            throw e;
        }
    }

    /**
     * Gets all the questions made by the user with given identifier
     * @param userId identifier of the user
     * @return List of questions made by the user
     */
    private List<QuestionDAO> userQuestions(String userId){
        return this.container
                .queryItems(
                        "SELECT * FROM questions WHERE questions.userId=\"" + userId + "\"",
                        new CosmosQueryRequestOptions(),
                        QuestionDAO.class)
                .stream().toList();
    }
}