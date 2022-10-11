package scc.data.database.CosmosDB;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import scc.data.database.CosmosDB.CosmosDBLayer;
import scc.data.database.ReplyDAO;

import java.util.Optional;

/**
 * Access to Replies database
 */
public class ReplyCosmosDBLayer extends CosmosDBLayer {

    private static final String REPLY_CONTAINER_NAMES = "replies";

    private final CosmosContainer replies;

    /**
     * Default constructor
     */
    public ReplyCosmosDBLayer(){
        this.replies = db.getContainer(REPLY_CONTAINER_NAMES);
    }

    /**
     * Adds an entry to the database with given reply
     * @param reply Question that is to be inserted into the database
     * @return Response of the creation of the entry
     */
    public CosmosItemResponse<Object> putReply(ReplyDAO reply){
        return replies.createItem(reply);
    }

    /**
     * Gets the reply that has a given identifier
     * @param id Identifier of the reply
     * @return Reply with same identifier or none if not present in the database
     */
    public Optional<ReplyDAO> getReplyById(String id){
        return replies.queryItems("SELECT * FROM replies WHERE replies.id=\"" + id + "\"", new CosmosQueryRequestOptions(),
                ReplyDAO.class).stream().findFirst();
    }
}
