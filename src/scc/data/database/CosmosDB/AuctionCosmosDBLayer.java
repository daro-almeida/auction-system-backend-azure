package scc.data.database.CosmosDB;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.database.AuctionDAO;

import java.util.Optional;

/**
 * Access to Auctions database
 */
public class AuctionCosmosDBLayer extends CosmosDBLayer {

    private static final String AUCTIONS_CONTAINER_NAME = "auctions";

    private final CosmosContainer auctions;

    /**
     * Default constructor
     */
    public AuctionCosmosDBLayer(){
        super();
        auctions = db.getContainer(AUCTIONS_CONTAINER_NAME);
    }

    /**
     * Adds an entry to the database with the given auction
     * @param auction Auction that is to be inserted to the database
     * @return Response of the creation of the entry
     */
    public CosmosItemResponse<Object> putAuction(AuctionDAO auction){
        return auctions.createItem(auction);
    }

    /**
     * Gets an entry in the database that has the given id
     * @param id Identifier of the auction
     * @return Entry of the auction with same id or null if not present in the database
     */
    public Optional<AuctionDAO> getAuctionById(String id){
        return auctions.queryItems("SELECT * FROM auctions WHERE auctions.id=\"" + id + "\"", new CosmosQueryRequestOptions(),
                AuctionDAO.class).stream().findFirst();
    }

    /**
     * Lists all the auctions that are about to close
     * @return Object that contains the auctions that meet the condition above
     */
    public CosmosPagedIterable<AuctionDAO> getAuctionsAboutToClose(){
        //TODO: Define "about to close"
        return auctions.queryItems("SELECT * FROM auctions", new CosmosQueryRequestOptions(), AuctionDAO.class);
    }

    /**
     * Lists all the auctions from a given user
     * @param userId Identifier of the user
     * @return List of auctions to which the user is the owner
     */
    public CosmosPagedIterable<AuctionDAO> getAuctionsByUser(String userId){
        return auctions.queryItems("SELECT * FROM auctions WHERE auctions.userId =\"" + userId + "\"", new CosmosQueryRequestOptions(), AuctionDAO.class);
    }

    /**
     * Updates an auction present in the database with new values
     * @param id Identifier of the auction
     * @param operations Values that are to be changed in the auction entry
     * @return Response of the updates performed in the entry
     */
    public CosmosItemResponse<AuctionDAO> updateAuction(String id, CosmosPatchOperations operations){
        return auctions.patchItem(id, new PartitionKey(id), operations, AuctionDAO.class);
    }
}
