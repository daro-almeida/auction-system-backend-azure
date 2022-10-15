package scc.azure;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.QuestionDAO;
import scc.services.AuctionService;
import scc.utils.Result;

class QuestionDB {
    private final CosmosContainer container;

    public QuestionDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.questionContainer);
    }

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

    public boolean questionExists(String questionId) {
        return this.getQuestion(questionId).isPresent();
    }

    public Result<QuestionDAO, AuctionService.Error> createQuestion(QuestionDAO question) {
        assert question.getQuestionId() == null; // Auto-generated
        var response = this.container.createItem(question);
        return Result.ok(response.getItem());
    }

    public Result<QuestionDAO, AuctionService.Error> createReply(String questionId, QuestionDAO.Reply reply) {
        var question = this.getQuestion(questionId);
        if (question.isEmpty())
            return Result.error(AuctionService.Error.QUESTION_NOT_FOUND);
        if (question.get().getReply() != null)
            return Result.error(AuctionService.Error.QUESTION_ALREADY_REPLIED);

        var partitionKey = this.createPartitionKey(questionId);
        var patch = CosmosPatchOperations.create();
        patch.add("/reply", reply);
        var response = this.container.patchItem(questionId, partitionKey, patch, QuestionDAO.class);

        return Result.ok(response.getItem());
    }

    public List<QuestionDAO> listQuestions(String auctionId) {
        var options = this.createQueryOptions(auctionId);
        return this.container
                .queryItems(
                        "SELECT * FROM questions WHERE questions.auctionId=\"" + auctionId + "\"",
                        options,
                        QuestionDAO.class)
                .stream().collect(Collectors.toList());
    }

    private PartitionKey createPartitionKey(String questionId) {
        return new PartitionKey(questionId);
    }

    private CosmosQueryRequestOptions createQueryOptions(String questionId) {
        var options = new CosmosQueryRequestOptions();
        options.setPartitionKey(this.createPartitionKey(questionId));
        return options;
    }
}