package scc.data.database.CosmosDB;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.database.CosmosDB.CosmosDBLayer;
import scc.data.database.QuestionDAO;

import java.util.Optional;

/**
 * Access to Questions database
 */
public class QuestionCosmosDBLayer extends CosmosDBLayer {

    private static final String QUESTION_CONTAINER_NAME = "questions";

    private final CosmosContainer questions;

    /**
     * Default constructor
     */
    public QuestionCosmosDBLayer(){
        this.questions = db.getContainer(QUESTION_CONTAINER_NAME);
    }

    /**
     * Adds an entry to the database with given question
     * @param question Question that is to be inserted into the database
     * @return Response of the creation of the entry
     */
    public CosmosItemResponse<Object> putQuestion(QuestionDAO question){
        return questions.createItem(question);
    }

    /**
     * Lists all questions associated with a given auction
     * @param auctionId Identifier of the auction
     * @return List of questions that were asked in the auction
     */
    public CosmosPagedIterable<QuestionDAO> listQuestionsByAuctionId(String auctionId){
        return questions.queryItems("SELECT * FROM questions WHERE questions.auctionId =\"" + auctionId + "\"", new CosmosQueryRequestOptions(),
                QuestionDAO.class);
    }

    /**
     * Gets the question associated with a given identifier
     * @param id Identifier of the question
     * @return Question with same identifier or none if not present in the database
     */
    public Optional<QuestionDAO> getQuestionById(String id){
        return questions.queryItems("SELECT * FROM questions WHERE questions.id=\"" + id + "\"", new CosmosQueryRequestOptions(),
                QuestionDAO.class).stream().findFirst();
    }
}
